package  com.resonance.cashdisplay.web;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
//import android.util.Log;

import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.PreferenceParams;
import com.resonance.cashdisplay.PreferencesValues;

import com.resonance.cashdisplay.eth.Eth_Settings;
import com.resonance.cashdisplay.web.StaticContentRestlet;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Directory;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.MapVerifier;
import org.restlet.security.SecretVerifier;
import org.restlet.security.Verifier;
import org.restlet.util.Series;
import org.restlet.util.ServerList;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;

import static com.resonance.cashdisplay.web.WebStatus.CLEAR_QUEUE_WEB_MESSAGE;

public class WebServer extends ServerResource {

    private StaticContentRestlet staticContentRestlet;
    private static final String TAG = "WebServer";
    private static Context mContext ;
    public WebStatus webStatus;

    private static  Component component;


    //https://maxrohde.com/2011/09/02/restlet-quickstart/

    private final String ROOT_URI = "file:///storage/sdcard2/Documents/web/";//index.html";


    public WebServer(Context context) {
        mContext = context;
        webStatus = new WebStatus();
    }


    public synchronized void SendQueWebStatus(String str_msg, boolean clearQueue){
            Message msg = new Message();
            msg.what = WebStatus.SEND_TO_QUEUE_WEB_MESSAGE;
            msg.obj = str_msg;
            msg.arg1 = (clearQueue?CLEAR_QUEUE_WEB_MESSAGE:0);
            msg.arg2 = 0;
            Handler h = webStatus.getWeb_message_handler();
            if (h!=null)
             webStatus.getWeb_message_handler().sendMessage(msg);
    }




    public void runWebServer(){
    //  Log.e("SSS", "URI:"+ROOT_URI);

        try {
            // Create a new Component.
            component = new Component();
            staticContentRestlet = new StaticContentRestlet();//component.getContext(
            // Add a new HTTP server listening on port 8182.
            component.getServers().add(Protocol.HTTP, 8182);

            final Series<Parameter> parameters = component.getContext().getParameters();
           // parameters.add("maxThreads", "50");
          //  parameters.add("minThreads", "10");
          //  parameters.add("lowThreads", "100");
          //  parameters.add("maxQueued", "10");
            parameters.add("maxTotalConnections", "1");
            parameters.add("maxConnectionsPerHost", "1");

            component.getContext().setParameters(parameters);


           // component.getServers().getContext().getParameters().add("requestBufferSize", "8192");
           // component.getServers().getContext().getParameters().add("maxTotalConnections", "1");

           // component.getContext().setParameters(parameters);

          /*  Log.d(TAG, " parameters.size() :"+ parameters.size());
            for (int i=0;i< parameters.size();i++){
                Log.d(TAG, "parameters :"+parameters.get(i).getName());
            }
*/


         /*   final Router router = new Router(component.getContext().createChildContext());
            router.attach("/test", WebServer.class);
            router.attach("{uid}", staticContentRestlet); // Filename is the static file to send to the user's browse
            component.getDefaultHost().attach("/", router);
*/



          /*  final Restlet restlet = new Restlet() {
                @Override
                public void handle(Request request, Response response) {
                    response.setEntity(new StringRepresentation("hello, world", MediaType.TEXT_PLAIN));
                   // response.setEntity (new Directory(getContext(), ROOT_URI));

                }
            };
*/



      /*************************************************************************/


            Application app = new Application()
            {
               @Override
               public Restlet createInboundRoot() {


                   PreferenceParams prefParams = new PreferenceParams();
                   PreferencesValues prefValues = prefParams.getParameters();

                   Log.w(TAG, "Start WEB, usr:"+prefValues.sAdmin+" psw:"+prefValues.sAdminPassw+" "+component.getServers().get(0).getName());
                   // Create a simple password verifier
                   MapVerifier verifier = new MapVerifier();
                   //получим настройки
                   verifier.getLocalSecrets().put(prefValues.sAdmin, prefValues.sAdminPassw.toCharArray());

                   // Create a guard
                   ChallengeAuthenticator guard = new ChallengeAuthenticator(getContext(),
                           ChallengeScheme.HTTP_BASIC, "Realm");
                   guard.setVerifier(verifier);


                      //guard.setNext(staticContentRestlet);
                      //guard.setNext(restlet);
                      // Create a Directory able to return a deep hierarchy of files
                    /*  Directory directory = new Directory(getContext(), ROOT_URI);
                      directory.setListingAllowed(true);
                      directory.setIndexName("index.html");*/
                     // guard.setNext(directory);

                     guard.setNext(staticContentRestlet);

                      return guard;
               }

         };
            // Attach the application to the component and start it
            component.getDefaultHost().attach(app);
//            component.start();


          //  component.getDefaultHost().attach(new WebApplication());
            //component.getDefaultHost().detach(WebApplication.class);
/****************************************************************/


            // Start the component.
            final Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        component.start();

                        Log.d(TAG, "Web server started : "+ Eth_Settings.getNetworkInterfaceIpAddress());
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                    super.run();
                }
            };
            t.run();

        } catch (Exception e) {
            Log.e(TAG, "WEB server Exception: "+e.getMessage());

        }
      //  String ip = EthernetSettings.getNetworkInterfaceIpAddress();// getLocalIpAddress();
     //   Log.d(TAG, "Local IP: "+ip);

    }







}