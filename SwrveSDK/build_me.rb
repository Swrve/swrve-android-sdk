# There is a big bug in the maven gradle task for uploadArchives which stops pom files being generated for flavored android libraries.
# This script will be used to parse the build.gradle, and for each flavor build a pom file and build it
require 'nokogiri'
require 'fileutils'

class PomFile
  attr_accessor :location, :body
end

class Dependency
  attr_accessor :flavor_name, :group_id, :artifact_id, :version

  def self.from_remote_compile_string(str)
    deets = str.split(":")
    dep = Dependency.new
    dep.group_id, dep.artifact_id, dep.version = deets[0], deets[1], deets[2]
    dep
  end
end

class String
  def string_between_markers marker1, marker2
    self[/#{Regexp.escape(marker1)}(.*?)#{Regexp.escape(marker2)}/m, 1]
  end
end

class Flavor
  attr_accessor :name, :artifact_id, :group_id, :version

  def initialize(name, art_id, group_id, version)
    self.name = name
    self.artifact_id = art_id
    self.group_id= group_id
    self.version = version
  end
end

# Run the upload archives task
puts "Assembling AARS"
# `../gradlew clean build assembleRelease uploadArchives`
`../gradlew uploadArchives`

# Examine the inner elements of the gradle file
path = './build.gradle'
gradle_build_str = IO.read(path)

puts "Calculating flavors (hardcoded)"
group_id = 'com.swrve.sdk.android'
# Count the flavors, versions and artifact IDS # Hardcoded for now
product_flavors = [
  Flavor.new('vanilla', 'swrve', group_id, '4.0.0'),
  Flavor.new('google', 'swrve-google', group_id, '4.0.0')
]

puts "Calculating Dependencies \n"
# Grab the dependecy list
dependencies_arr = gradle_build_str.string_between_markers('dependencies {', '}').split("\n").map { |str| str.strip }.reject { |str| str.eql?("") }
dependencies = dependencies_arr.map do |dep_str|
  dependency_details = dep_str.string_between_markers("'", "'")
  dep = Dependency.from_remote_compile_string(dependency_details)

  if !dep_str.start_with?("compile")
    # Dependency is flavored
    dep.flavor_name = dep_str.string_between_markers("", "Compile")
  end
  dep
end

# Where are libs published
repo_location = gradle_build_str.string_between_markers "repository(url: '", "')"
repo_location.gsub!('..//', '') # An unfortunate requirement to strip this from the directory location or else we get dir not found error.


puts "Building Poms for flavors! \n"
# For every flavor
product_flavors.each do |flavor|
  group_id = flavor.group_id # Get the group ID
  # Find the location of where its uploaded
  group_id_location = "/" + group_id.gsub(".", "/") + "/"
  pomfile = PomFile.new
  pomfile.location = repo_location + group_id_location + flavor.artifact_id + '/' + flavor.version + '/'

  builder = Nokogiri::XML::Builder.new do |xml|
    xml.project('xsi:schemaLocation' => "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd") {
      xml.modelVersion flavor.version
      xml.groupId flavor.group_id
      xml.artifactId flavor.artifact_id
      xml.version flavor.version
      xml.packaging 'aar'
      xml.dependencies {
        dependencies.each do |dep|
          if dep.flavor_name and !dep.flavor_name.eql?(flavor.name)
            # If the dependency has a flavor requirement and its the same as the current flavor, add it or move on
            next
          end
          xml.dependency {
            xml.groupId dep.group_id
            xml.artifactId dep.artifact_id
            xml.version dep.version
            xml.scope 'compile'
          }
        end
      }
    }
  end
  pomfile.body = builder.to_xml

  pomfile_name = "#{flavor.artifact_id}-#{flavor.version}.pom"

  FileUtils.cd(pomfile.location) do ||
    begin
      FileUtils.rm(pomfile_name)
    rescue => e
      # This is fine because there is no file there
    end


    File.open(pomfile_name, 'wb') do |f|
      f.write(pomfile.body)
      f.close
    end
  end
end