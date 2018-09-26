package com.talagasoft.gojekdriver;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.talagasoft.gojekdriver.model.OrderRecord;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by compaq on 01/18/2017.
 */
public class TransactionAdapter implements ListAdapter {
    Context _context;
    String _hp;
    ArrayList<OrderRecord> _orders;
    TextView tanggal,amount,km,tujuan;

    private static LayoutInflater inflater=null;

    public TransactionAdapter(Context context, String mNomorHp) {
        this._context=context;
        this._hp=mNomorHp;
        _orders=new OrderRecord().getList(context,mNomorHp);
        inflater = ( LayoutInflater )_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }
    @Override

    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int i) {
        return false;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {

    }

    @Override
    public int getCount() {

        if(_orders.size()>0){
            return _orders.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int i) {
        return _orders.get(i);
    }

    @Override
    public long getItemId(int i) {
        return _orders.get(i).get_id();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        View vi = convertView;
        if(convertView==null){
            vi = inflater.inflate(R.layout.order_row, null);
            tanggal=(TextView)vi.findViewById(R.id.tanggal);
            amount=(TextView)vi.findViewById(R.id.amount);
            km=(TextView)vi.findViewById(R.id.km);
            tujuan=(TextView)vi.findViewById(R.id.tujuan);
        }
        OrderRecord order=new OrderRecord();
        order=_orders.get(position);
        if (order != null) {
            tanggal.setText(order.get_tgl());
            DecimalFormat df = new DecimalFormat("###,###.##");
            amount.setText("Rp. " + df.format(order.get_amount()));
            km.setText("" + df.format(order.get_km()) + " Km");
            tujuan.setText("" + order.get_to_lat() + "/" + order.get_to_lng());
        } else {
            tanggal.setText("");
            amount.setText("Rp. 0");
            km.setText("");
            tujuan.setText("");
        }
        return vi;
    }

    @Override
    public int getItemViewType(int i) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        if(_orders.size()>0){
            return _orders.size();
        } else {
            return 1;
        }
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
