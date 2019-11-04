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
import java.util.Locale;


public class AdapterProductList extends ArrayAdapter<ItemShoppingList> implements View.OnClickListener {

    private final static String TAG = "ShoppingListActivity";
    Context mContext;
    private int listItemResourceId;

    private class ViewHolder {
        TextView textview_npp;
        TextView textview_tovar;
        TextView textview_Count;                // value received from COM port
        TextView textview_Price;                // value received from COM port
        TextView textViewSummWithoutDiscount;   // this value will be calculated
        TextView textViewDiscount;              // this value will be calculated
        TextView textview_Summ;                 // value received from COM port
        ImageView imageview_icon;
    }

    public AdapterProductList(Context context, int resource, ArrayList<ItemShoppingList> data) {
        super(context, resource, data);
        this.mContext = context;
        this.listItemResourceId = resource;
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick :+position");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Получить элемент данных для этой позиции
        ItemShoppingList dataModel = getItem(position);

        final AdapterProductList.ViewHolder viewHolder;
        final View resultView;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(listItemResourceId, parent, false);
            viewHolder = new AdapterProductList.ViewHolder();
            viewHolder.textview_npp = (TextView) convertView.findViewById(R.id.textview_npp);
            viewHolder.textview_tovar = (TextView) convertView.findViewById(R.id.textview_tovar);
            viewHolder.textview_Count = (TextView) convertView.findViewById(R.id.textview_Count);
            viewHolder.textview_Price = (TextView) convertView.findViewById(R.id.textview_Price);
            viewHolder.textViewSummWithoutDiscount = (TextView) convertView.findViewById(R.id.textview_summ_without_discount);
            viewHolder.textViewDiscount = (TextView) convertView.findViewById(R.id.textview_discount);
            viewHolder.textview_Summ = (TextView) convertView.findViewById(R.id.textview_Summ);
            viewHolder.imageview_icon = (ImageView) convertView.findViewById(R.id.imageview_icon);
            convertView.setTag(viewHolder);
            resultView = convertView;
        } else {
            viewHolder = (AdapterProductList.ViewHolder) convertView.getTag();
            resultView = convertView;
        }
        viewHolder.textview_npp.setText("" + (position + 1));
        viewHolder.textview_tovar.setText(dataModel.getNameTovara());
        viewHolder.textview_Count.setText(((dataModel.getDivisible() == 1) ? (String.format("%.03f", (float) dataModel.getCount() / 1000)) : ("" + dataModel.getCount())).replace(",", "."));
        viewHolder.textview_Price.setText(String.format("%.02f", (float) ((float) dataModel.getPrice() / 100)).replace(",", "."));
        viewHolder.textViewSummWithoutDiscount.setText(String.format(Locale.ROOT,"%.02f", (float) ((float) dataModel.getSumWithoutDiscount() / 100)));
        viewHolder.textViewDiscount.setText(String.format(Locale.ROOT,"%.02f", (float) ((float) dataModel.getDiscount() / 100)));
        viewHolder.textview_Summ.setText(String.format("%.02f", (float) ((float) dataModel.getSumm() / 100)).replace(",", "."));
        viewHolder.textview_tovar.setTag(position);
        return resultView;          // here was convertView initially, mistake ????
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

