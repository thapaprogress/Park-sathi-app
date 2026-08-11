package woyou.aidl;

import woyou.aidl.ICallback;
import android.graphics.Bitmap;

interface IWoyouService {
    void printerInit(in ICallback callback);
    void printText(String text, in ICallback callback);
    void setAlignment(int alignment, in ICallback callback);
    void setPrinterFontSize(float fontSize, in ICallback callback);
    void printBitmap(in Bitmap bitmap, in ICallback callback);
    void printQRCode(String data, int modulesize, int errorlevel, in ICallback callback);
    void sendRAWData(in byte[] data, in ICallback callback);
    void lineWrap(int lines, in ICallback callback);
}
