package com.talagasoft.gojekdriver.model;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.talagasoft.gojekdriver.R;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by compaq on 01/13/2017.
 */

public class Penumpang {
    Context _context;
    String _msg="";
    String _url="";
    private String  TAG  = "Penumpang";
    private String mNoHpTo;

    public Penumpang(Context c) {
        _context=c;
        _url=_context.getResources().getString(R.string.url_source);
    }
    public Penumpang(){
        _url=_context.getResources().getString(R.string.url_source);
    }

    public boolean AcceptOrder(String mNomorHp, String mNoHpTo) {

        String mUrl=_url +"order_accept.php?handphone=" + mNomorHp + "&to=" + mNoHpTo;

        boolean lTrue=false;
        _msg="";
        HttpXml web=new HttpXml();
        StringBuilder doc=web.GetUrlData(mUrl);
        if(doc != null) {
            if(doc.toString().contains("success")) {
                lTrue=true;
            }
            _msg=doc.toString();
        }
        return lTrue;
    }
    public String getError(){
        return _msg;
    }

    public int Saldo(String mNoHp){
        String mUrl=_url +"deposit_saldo.php?handphone="+mNoHp;
        HttpXml web=new HttpXml(mUrl);
        return Integer.parseInt(web.getKey("saldo"));
    }

    public List<PenumpangRecord> getAllNewOrder(String mNomorHp, LatLng myLatLng) {
        String mUrl = _url + "order_list.php?hp=" + mNomorHp + "&lat=" + myLatLng.latitude + "&lng=" + myLatLng.longitude;
        HttpXml web = new HttpXml();
        Document doc = web.GetUrl(mUrl);
        List<PenumpangRecord> arPenumpang = new ArrayList<PenumpangRecord>();
        if (doc != null) {
            Log.d(TAG, doc.toString());
            NodeList nl1, nl2, nl3;
            nl1 = doc.getElementsByTagName("people");
            if (nl1.getLength() > 0) {
                for (int i = 0; i < nl1.getLength(); i++) {
                    Node node1 = nl1.item(i);
                    nl2 = node1.getChildNodes();
                    Node latNode = nl2.item(web.getNodeIndex(nl2, "lat"));
                    double lat = Double.parseDouble(latNode.getTextContent());
                    Node lngNode = nl2.item(web.getNodeIndex(nl2, "lng"));
                    double lng = Double.parseDouble(lngNode.getTextContent());
                    Node hpNode = nl2.item(web.getNodeIndex(nl2, "handphone"));
                    String hp = String.valueOf(hpNode.getTextContent());
                    Node namaNode = nl2.item(web.getNodeIndex(nl2, "user_name"));
                    String nama = String.valueOf(namaNode.getTextContent());
                    Node toLatNode = nl2.item(web.getNodeIndex(nl2, "to_lat"));
                    double to_lat = Double.parseDouble(toLatNode.getTextContent());
                    Node toLngNode = nl2.item(web.getNodeIndex(nl2, "to_lng"));
                    double to_lng = Double.parseDouble(toLngNode.getTextContent());

                    arPenumpang.add(new PenumpangRecord(1, nama, lat, lng, hp, 1,to_lat,to_lng));
                    Log.d("getAllNewOrder","Nama="+nama+", lat="+lat+", lng="+lng+", hp="+hp+", to_lat="+to_lat+", to_lng="+to_lng);
                }
            }
        }
        return arPenumpang;
    }

    public boolean FinishOrder(String mNoHp) {
        String mUrl=_url +"order_finish.php?handphone=" + mNoHp;

        boolean lTrue=false;
        _msg="";
        HttpXml web=new HttpXml();
        StringBuilder doc=web.GetUrlData(mUrl);
        if(doc != null) {
            if(doc.toString().contains("success")) {
                lTrue=true;
            }
            _msg=doc.toString();
        }
        return lTrue;
    }
}

