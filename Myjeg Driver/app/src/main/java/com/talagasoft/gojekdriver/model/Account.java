package com.talagasoft.gojekdriver.model;

import android.content.Context;

import com.talagasoft.gojekdriver.R;

import org.w3c.dom.Document;

/**
 * Created by compaq on 01/18/2017.
 */

public class Account {
    Context _context;
    public  Account(Context _context){

    }
    public boolean getById(String sNoHp){
        boolean lRet=false;
        String url=_context.getResources().getString(R.string.url_source);

        String mUrl = url + "order_trans_list.php?hp=" + sNoHp;
        HttpXml web = new HttpXml();
        Document doc = web.GetUrl(mUrl);

        return lRet;
    }
}
