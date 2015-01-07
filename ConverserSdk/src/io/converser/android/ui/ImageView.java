package io.converser.android.ui;

import android.content.Context;

import io.converser.android.engine.model.Content;
import io.converser.android.engine.model.ConversationAtom;

public class ImageView extends android.widget.ImageView implements ConverserContent {

    private Content model;
    private Thread downloader;

    public ImageView(Context context, Content model) {
        super(context);

        ImageDownloader imgDownloader = new ImageDownloader(this, model.getValue());
        downloader = new Thread(imgDownloader);
        downloader.start();
    }

    @Override
    public ConversationAtom getModel() {
        return model;
    }

}
