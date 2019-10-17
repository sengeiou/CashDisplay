
package com.resonance.cashdisplay.web;

//import android.util.Log;

import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.PreferenceParams;
import com.resonance.cashdisplay.PreferencesValues;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Protocol;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.MapVerifier;

public class WebApplication extends Application {

    private final String TAG = "WebApplication";
    private ChallengeAuthenticator authenticatior;
    StaticContentRestlet staticContentRestlet = new StaticContentRestlet();//component.getContext(

//https://stackoverflow.com/questions/2217418/fine-grained-authentication-with-restlet

    private ChallengeAuthenticator createAuthenticator() {
        Context context = getContext();
        boolean optional = true;
        ChallengeScheme challengeScheme = ChallengeScheme.HTTP_BASIC;
        String realm = "Example site";



        PreferenceParams prefParams = new PreferenceParams();
        PreferencesValues prefValues = prefParams.getParameters();
        Log.e(TAG, " usr:"+prefValues.sAdmin+" psw:"+prefValues.sAdminPassw);
        /**************************************************/
        // MapVerifier isn't very secure; see docs for alternatives
        MapVerifier verifier = new MapVerifier();
        verifier.getLocalSecrets().put(prefValues.sAdmin, prefValues.sAdminPassw.toCharArray());

        ChallengeAuthenticator auth = new ChallengeAuthenticator(context, optional, challengeScheme, realm, verifier)
        {
            @Override
            protected boolean authenticate(Request request, Response response) {
                if (request.getChallengeResponse() == null) {
                    return false;
                } else {
                    return super.authenticate(request, response);
                }
            }
        };

        return auth;
    }

    @Override
    public Restlet createInboundRoot() {
        Log.d(TAG, "WebApplication-----createInboundRoot");
        this.authenticatior = createAuthenticator();

        Router router = new Router();
        router.attach("/",staticContentRestlet);

        authenticatior.setNext(staticContentRestlet);//(router);
        return authenticatior;
    }

    public boolean authenticate(Request request, Response response) {
        if (!request.getClientInfo().isAuthenticated()) {
            Log.d(TAG, "request.getClientInfo().isAuthenticated()");
            authenticatior.challenge(response, false);
            return false;
        }
        return true;
    }


    public boolean reauthenticate(Request request, Response response) {

        MapVerifier verifier = new MapVerifier();
        PreferenceParams prefParams = new PreferenceParams();
        PreferencesValues prefValues = prefParams.getParameters();
        verifier.getLocalSecrets().put(prefValues.sAdmin, prefValues.sAdminPassw.toCharArray());
        this.authenticatior.setVerifier(verifier);
        this.authenticatior.setRechallenging(true);
        authenticatior.challenge(response, true);
         return false;

    }

}