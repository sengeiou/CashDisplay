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

import androidx.constraintlayout.widget.ConstraintLayout;

import com.resonance.cashdisplay.ExtSDSource;
import com.resonance.cashdisplay.MainActivity;
import com.resonance.cashdisplay.R;
import com.resonance.cashdisplay.load.DownloadMedia;
import com.resonance.cashdisplay.product_list.look2.KievSubwayArgs;
import com.resonance.cashdisplay.settings.PrefValues;
import com.resonance.cashdisplay.settings.PrefWorker;
import com.resonance.cashdisplay.utils.ImageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import static com.resonance.cashdisplay.settings.PrefWorker.LOOK_SUBWAY;


public class AdapterProductList extends ArrayAdapter<ItemProductList> {

    private final static String TAG = "AdapterProductList";
    Context mContext;
    private int listItemResourceId;

    private class ViewHolder {
        TextView textViewN;
        TextView textViewProduct;
        TextView textViewCount;                // value received from COM port
        TextView textViewPrice;                // value received from COM port
        TextView textViewSumWithoutDiscount;   // this value will be calculated
        TextView textViewDiscount;             // this value will be calculated
        TextView textViewSum;                  // value received from COM port
        ImageView imageViewIcon;

        // used for LOOK_SUBWAY only
        ConstraintLayout layoutSum;
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
            viewHolder.textViewN = (TextView) convertView.findViewById(R.id.textview_npp);
            viewHolder.textViewProduct = (TextView) convertView.findViewById(R.id.textview_product);
            viewHolder.textViewCount = (TextView) convertView.findViewById(R.id.textview_count);
            viewHolder.textViewPrice = (TextView) convertView.findViewById(R.id.textview_price);
            viewHolder.textViewSumWithoutDiscount = (TextView) convertView.findViewById(R.id.textview_sum_without_discount);
            viewHolder.textViewDiscount = (TextView) convertView.findViewById(R.id.textview_discount);
            viewHolder.textViewSum = (TextView) convertView.findViewById(R.id.textview_sum);
            viewHolder.imageViewIcon = (ImageView) convertView.findViewById(R.id.imageview_icon);
            viewHolder.layoutSum = (ConstraintLayout) convertView.findViewById(R.id.layout_sum);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (AdapterProductList.ViewHolder) convertView.getTag();
        }

        viewHolder.textViewN.setText("" + (position + 1));
        viewHolder.textViewProduct.setText(dataModel.getName());
        viewHolder.textViewCount.setText((dataModel.getDivisible() == 1) ? (String.format(Locale.ROOT, "%.03f", (double) dataModel.getCount() / 1000)) : (String.valueOf(dataModel.getCount())));
        viewHolder.textViewPrice.setText(String.format(Locale.ROOT, "%.02f", (double) dataModel.getPrice() / 100));
        viewHolder.textViewSumWithoutDiscount.setText(String.format(Locale.ROOT, "%.02f", (double) dataModel.getSumWithoutDiscount() / 100));
        viewHolder.textViewDiscount.setText(String.format(Locale.ROOT, "%.02f", (double) dataModel.getDiscount() / 100));
        viewHolder.textViewSum.setText(String.format(Locale.ROOT, "%.02f", (double) dataModel.getSum() / 100));
        viewHolder.textViewProduct.setTag(position);

        switch (PrefWorker.getValues().productListLookCode) {
            case LOOK_SUBWAY:
                if (dataModel.getCount() < 0)
                    viewHolder.textViewCount.setText(R.string.unlimited);
                if (KievSubwayArgs.isCharging) {
                    viewHolder.textViewSum.setText(String.format(Locale.FRENCH, "%.2f", (double) dataModel.getSum() / 100));
                    viewHolder.layoutSum.setVisibility(View.VISIBLE);
                }
                else
                    viewHolder.layoutSum.setVisibility(View.GONE);
                break;
            default:
                break;
        }

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
                PrefValues prefValues = PrefWorker.getValues();
                filepath = ExtSDSource.getExternalSdCardPath() + DownloadMedia.IMG_SCREEN + ((prefValues.defaultBackgroundImage.length() > 0) ? prefValues.defaultBackgroundImage : "noimg");
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

