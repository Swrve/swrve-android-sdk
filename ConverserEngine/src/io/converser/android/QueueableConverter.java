package io.converser.android;

import com.google.ciogson.Gson;
import com.squareup.tape.FileObjectQueue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

class QueueableConverter implements FileObjectQueue.Converter<Queueable> {

    private Gson gson;

    public QueueableConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Queueable from(byte[] bytes) throws IOException {
        Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes));
        return gson.fromJson(reader, Queueable.class);
    }

    @Override
    public void toStream(Queueable object, OutputStream bytes) throws IOException {
        Writer writer = new OutputStreamWriter(bytes);
        gson.toJson(object, writer);
        writer.close();
    }

}
