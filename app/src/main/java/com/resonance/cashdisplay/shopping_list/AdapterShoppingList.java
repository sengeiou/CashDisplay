package com.resonance.cashdisplay.shopping_list;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.resonance.cashdisplay.ExtSDSource;
import com.resonance.cashdisplay.Log;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.PreferenceParams;
import com.resonance.cashdisplay.PreferencesValues;
import com.resonance.cashdisplay.R;
import com.resonance.cashdisplay.load.DownloadMedia;
import com.resonance.cashdisplay.utils.ImageUtils;

import java.io.File;
import java.util.ArrayList;


public class AdapterShoppingList extends ArrayAdapter<ItemShoppingList> implements View.OnClickListener {

    private final static String TAG = "ShoppingListActivity";
    private ArrayList<ItemShoppingList> dataSet;
    Context mContext;
    boolean inverse = false;


    private class ViewHolder {

        TextView textview_tovar;
        TextView textview_Count;
        TextView textview_Price;
        TextView textview_Summ;
        TextView textview_npp;
        ImageView imageview_icon;
    }

    public AdapterShoppingList(ArrayList<ItemShoppingList> data, Context context) {
        super(context, R.layout.list_item, data);
        this.dataSet = data;
        this.mContext = context;
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick :+position");
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Получить элемент данных для этой позиции
        ItemShoppingList dataModel = getItem(position);

        final AdapterShoppingList.ViewHolder viewHolder;
        final View result;

        if (convertView == null) {

            viewHolder = new AdapterShoppingList.ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.list_item, parent, false);
            viewHolder.textview_npp = (TextView) convertView.findViewById(R.id.textview_npp);
            viewHolder.textview_tovar = (TextView) convertView.findViewById(R.id.textview_tovar);
            viewHolder.textview_Count = (TextView) convertView.findViewById(R.id.textview_Count);
            viewHolder.textview_Price = (TextView) convertView.findViewById(R.id.textview_Price);
            viewHolder.textview_Summ = (TextView) convertView.findViewById(R.id.textview_Summ);
            viewHolder.imageview_icon = (ImageView) convertView.findViewById(R.id.imageview_icon);
            result = convertView;
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (AdapterShoppingList.ViewHolder) convertView.getTag();
            result = convertView;
        }


        viewHolder.textview_npp.setText("" + (position + 1));
        viewHolder.textview_tovar.setText(dataModel.getNameTovara());
        viewHolder.textview_Count.setText(((dataModel.getDivisible() == 1) ? (String.format("%.03f", (float) dataModel.getCount() / 1000)) : "" + dataModel.getCount()).replace(",", "."));
        viewHolder.textview_Price.setText(String.format("%.02f", (float) ((float) dataModel.getPrice() / 100)).replace(",", "."));
        viewHolder.textview_Summ.setText(String.format("%.02f", (float) ((float) dataModel.getSumm() / 100)).replace(",", "."));
        viewHolder.textview_tovar.setTag(position);


        return convertView;
    }

    /**
     * Получение изображения товара по коду товара из файлолвого хранилища
     *
     * @param codeProduct
     * @return
     */
    public static Bitmap getImage(String codeProduct) {

        String filepath = ExtSDSource.getExternalSdCardPath() + DownloadMedia.IMG_URI + codeProduct + ".png";//Изображение товара
        File fileImg = new File(filepath);

        if (fileImg.exists()) {// PNG
            return ImageUtils.getImage(fileImg, MainActivity.sizeScreen, false);
        } else {         //JPG
            filepath = ExtSDSource.getExternalSdCardPath() + DownloadMedia.IMG_URI + codeProduct + ".jpg";//Изображение товара
            fileImg = new File(filepath);
            if (fileImg.exists()) {
                return ImageUtils.getImage(fileImg, MainActivity.sizeScreen, false);
            } else {
                //покажем изображение по-умолчанию
                PreferencesValues prefValues = PreferenceParams.getParameters();
                filepath = ExtSDSource.getExternalSdCardPath() + DownloadMedia.IMG_SCREEN + ((prefValues.sDefaultBackGroundImage.length() > 0) ? prefValues.sDefaultBackGroundImage : "noimg");
                fileImg = new File(filepath);

                if (fileImg.exists()) {
                    return ImageUtils.getImage(fileImg, MainActivity.sizeScreen, false);
                } else {
                    return BitmapFactory.decodeResource(MainActivity.context.getResources(), R.drawable.noimagefound);
                }
            }
        }
    }
}

