package com.resonance.cashdisplay.product_list;

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
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.PreferenceParams;
import com.resonance.cashdisplay.PreferencesValues;
import com.resonance.cashdisplay.R;
import com.resonance.cashdisplay.load.DownloadMedia;
import com.resonance.cashdisplay.utils.ImageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;


public class AdapterProductList extends ArrayAdapter<ItemProductList> {

    private final static String TAG = "AdapterProductList";
    Context mContext;
    private int listItemResourceId;

    private class ViewHolder {
        TextView textviewN;
        TextView textviewProduct;
        TextView textviewCount;                // value received from COM port
        TextView textviewPrice;                // value received from COM port
        TextView textViewSumWithoutDiscount;   // this value will be calculated
        TextView textViewDiscount;              // this value will be calculated
        TextView textviewSum;                 // value received from COM port
        ImageView imageviewIcon;
    }

    public AdapterProductList(Context context, int resource, ArrayList<ItemProductList> data) {
        super(context, resource, data);
        this.mContext = context;
        this.listItemResourceId = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Получить элемент данных для этой позиции
        ItemProductList dataModel = getItem(position);

        final AdapterProductList.ViewHolder viewHolder;
        final View resultView;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(listItemResourceId, parent, false);
            viewHolder = new AdapterProductList.ViewHolder();
            viewHolder.textviewN = (TextView) convertView.findViewById(R.id.textview_npp);
            viewHolder.textviewProduct = (TextView) convertView.findViewById(R.id.textview_product);
            viewHolder.textviewCount = (TextView) convertView.findViewById(R.id.textview_count);
            viewHolder.textviewPrice = (TextView) convertView.findViewById(R.id.textview_price);
            viewHolder.textViewSumWithoutDiscount = (TextView) convertView.findViewById(R.id.textview_sum_without_discount);
            viewHolder.textViewDiscount = (TextView) convertView.findViewById(R.id.textview_discount);
            viewHolder.textviewSum = (TextView) convertView.findViewById(R.id.textview_sum);
            viewHolder.imageviewIcon = (ImageView) convertView.findViewById(R.id.imageview_icon);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (AdapterProductList.ViewHolder) convertView.getTag();
        }

        viewHolder.textviewN.setText("" + (position + 1));
        viewHolder.textviewProduct.setText(dataModel.getName());
        viewHolder.textviewCount.setText((dataModel.getDivisible() == 1) ? (String.format(Locale.ROOT, "%.03f", (double) dataModel.getCount() / 1000)) : (String.valueOf(dataModel.getCount())));
        viewHolder.textviewPrice.setText(String.format(Locale.ROOT, "%.02f", (double) dataModel.getPrice() / 100));
        viewHolder.textViewSumWithoutDiscount.setText(String.format(Locale.ROOT, "%.02f", (double) dataModel.getSumWithoutDiscount() / 100));
        viewHolder.textViewDiscount.setText(String.format(Locale.ROOT, "%.02f", (double) dataModel.getDiscount() / 100));
        viewHolder.textviewSum.setText(String.format(Locale.ROOT, "%.02f", (double) dataModel.getSum() / 100));
        viewHolder.textviewProduct.setTag(position);

        resultView = convertView;
        return resultView;
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

