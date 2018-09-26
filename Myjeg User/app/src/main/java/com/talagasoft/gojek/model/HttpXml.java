package com.talagasoft.gojek.model;

import android.os.StrictMode;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by compaq on 12/06/2016.
 */

public class HttpXml {

    StringBuilder stringBuilder;
    Document mDoc;
    String _url;
    private ArrayList<Node> _arNode;

    public HttpXml(String mUrl) {
        _url=mUrl;
        mDoc=GetUrl(_url);
    }
    public HttpXml(){

    }

    public Document GetUrl(String myUrl){
        Log.d("GetUrl",myUrl);
        stringBuilder = GetUrlData(myUrl);
        Document doc = null;
        try {
            doc=loadXMLFromString(String.valueOf(stringBuilder));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        return doc;

    }

    public StringBuilder GetUrlData(String myUrl) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {

            DefaultHttpClient client = new DefaultHttpClient();
            URL url = new URL(myUrl);
            URI website = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(),
                    url.getPath(), url.getQuery(), url.getRef());

            HttpGet request = new HttpGet();
            request.setURI(website);

            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            stringBuilder = new StringBuilder();
            InputStream stream = entity.getContent();
            int b;
            while ((b = stream.read()) != -1) {
                stringBuilder.append((char) b);
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        Log.d("HttpXml.GetUrlData()",myUrl + ": " + stringBuilder);
        return stringBuilder;
    }

    public static Document loadXMLFromString(String xml) throws IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        InputSource is = new InputSource(new StringReader(xml));

        return builder.parse(is);
    }
    public int getNodeIndex(NodeList nl, String nodename) {
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeName().equals(nodename))
                return i;
        }
        return -1;
    }

    public String getKey(String vKeyName) {
        String vResult="";
        if(mDoc==null && stringBuilder == null) return vResult;

        try {
            if(mDoc==null) {
                mDoc = loadXMLFromString(stringBuilder.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        if(mDoc != null){
            NodeList nl1;
            nl1 = mDoc.getElementsByTagName(vKeyName);
            if ( nl1 != null ) {
                Node node1 = nl1.item(0);
                if ( node1 != null ) {
                    vResult = node1.getTextContent();
                } else {
                    Log.d("getKey","Unknown Node !");
                }
            }
        }
        return vResult;
    }

    public float getKeyFloat(String vKey) {
        String s=getKey(vKey);
        if(s.contentEquals("")){
            return 0;
        }
        return Float.parseFloat(s);
    }

    public void getGroup(String vToken) {
        if(mDoc != null) {
            if(_arNode == null){
                _arNode=new ArrayList<>();
            }
            NodeList nl;
            nl = mDoc.getElementsByTagName(vToken);
            if(nl != null){
                for(int i=0;i<nl.getLength();i++){
                    _arNode.add(nl.item(i));
                }
            }
        }
    }

    public int getCount() {
        return _arNode.size();
    }

    public int getKeyInt(String vKey) {
        String s=getKey(vKey);
        if(s.isEmpty())s="0";
        return Integer.parseInt(s);
    }
    public float getKeyIndexFloat(int i, String vKeyName) {
        String v=getKeyIndex(i,vKeyName);
        if(v==""){
            v="0";
        }
        return Float.parseFloat(v);

    }
    public String getKeyIndex(int position, String vKeyName) {
        String vResult="";
        if(_arNode != null ) {
            NodeList nl = _arNode.get(position).getChildNodes();    //get row index
            for(int i=0;i<nl.getLength();i++){  //field name
                Node nd=nl.item(i);
                String vKey=nd.getNodeName();
                String vValue=nd.getNodeValue();
                if (vKey.contains(vKeyName)) {
                    Node nd2 = nd.getChildNodes().item(0);
                    if (nd2 != null) {
                        vResult = nd2.getTextContent();
                        return vResult;
                    }
                }

            }
            //Log.d("getKeyIndex.vResult",vResult);
        }
        return vResult;
    }

}
