package com.resonance.cashdisplay.web;

import android.content.res.Resources;

import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;

import org.restlet.representation.EmptyRepresentation;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;

import java.io.IOException;
import java.io.InputStream;

//import android.util.Log;

public class UserResource extends ServerResource {

    final String TAG = "UserResource";
    StaticContentRestlet staticContentRestlet = new StaticContentRestlet();//component.getContext(

    @Override
    public Representation get() {
        Log.d(TAG, "get() ");
        WebApplication app = (WebApplication) getApplication();
        if (!app.authenticate(getRequest(), getResponse())) {
            // Not authenticated
            Log.d(TAG, "Not authenticated");
            return new EmptyRepresentation();
        }

        Log.d(TAG, "authenticated OK");
        // Generate list of users
        try {

            Representation r = readStaticFile("index.html");
            return r;
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resources.NotFoundException:"+e.getMessage());

        } catch (IOException e) {
            Log.e(TAG, "IOException:"+e.getMessage());

        }

        return new EmptyRepresentation();//88888888888888
    }

    @Override
    public Representation post(Representation entity) {

        Log.d(TAG, "post() ");
        // Handle post
        // ...
        Log.d(TAG, "Representation post");
        return new EmptyRepresentation();//8888888888888888888
    }

    public Representation readStaticFile(String fileName) throws Resources.NotFoundException, IOException
    {
        InputStream is = MainActivity.context.getAssets().open(fileName);
        Representation representation = new InputRepresentation(is);
        return representation;
    }

}