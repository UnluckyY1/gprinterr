package com.example.bluetooth_print;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import com.gprinter.command.CpclCommand;
import com.gprinter.command.EscCommand;
import com.gprinter.command.LabelCommand;

import java.util.List;
import java.util.Map;
import java.util.Vector;

public class PrintContent {
      private static final String TAG = PrintContent.class.getSimpleName();

      /**
       * 票据打印对象转换
       * @return
       */
      public static Vector<Byte> mapToReceipt(Map<String,Object> config, List<Map<String,Object>> list) {
            EscCommand esc = new EscCommand();
            //初始化打印机
            esc.addInitializePrinter();
            //打印走纸多少个单位
            //esc.addPrintAndFeedLines((byte) 3);

            // {type:'text|barcode|qrcode|image', content:'', size:4, align: 0|1|2, weight: 0|1, width:0|1, height:0|1, underline:0|1, linefeed: 0|1}
            for (Map<String,Object> m: list) {
                  String type = (String)m.get("type");
                  String content = (String)m.get("content");
                  int align = (int)(m.get("align")==null?0:m.get("align"));
                  int size = (int)(m.get("size")==null?4:m.get("size"));
                  int weight = (int)(m.get("weight")==null?0:m.get("weight"));
                  int width = (int)(m.get("width")==null?0:m.get("width"));
                  int height = (int)(m.get("height")==null?0:m.get("height"));
                  int underline = (int)(m.get("underline")==null?0:m.get("underline"));
                  int linefeed = (int)(m.get("linefeed")==null?0:m.get("linefeed"));

                  EscCommand.ENABLE emphasized = weight==0?EscCommand.ENABLE.OFF:EscCommand.ENABLE.ON;
                  EscCommand.ENABLE doublewidth = width==0?EscCommand.ENABLE.OFF:EscCommand.ENABLE.ON;
                  EscCommand.ENABLE doubleheight = height==0?EscCommand.ENABLE.OFF:EscCommand.ENABLE.ON;
                  EscCommand.ENABLE isUnderline = underline==0?EscCommand.ENABLE.OFF:EscCommand.ENABLE.ON;

                  // 设置打印位置
                  esc.addSelectJustification(align==0?EscCommand.JUSTIFICATION.LEFT:(align==1?EscCommand.JUSTIFICATION.CENTER:EscCommand.JUSTIFICATION.RIGHT));

                  if("text".equals(type)){
                        // 设置为倍高倍宽
                        esc.addSelectPrintModes(EscCommand.FONT.FONTA, emphasized, doubleheight, doublewidth, isUnderline);
                        esc.addText(content);
                        // 取消倍高倍宽
                        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
                  }else if("barcode".equals(type)){
                        esc.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.BELOW);
                        // 设置条码可识别字符位置在条码下方
                        // 设置条码高度为60点
                        esc.addSetBarcodeHeight((byte) 60);
                        // 设置条码宽窄比为2
                        esc.addSetBarcodeWidth((byte) 2);
                        // 打印Code128码
                        esc.addCODE128(esc.genCodeB(content));
                  }else if("qrcode".equals(type)){
                        // 设置纠错等级
                        esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31);
                        // 设置qrcode模块大小
                        esc.addSelectSizeOfModuleForQRCode((byte) size);
                        // 设置qrcode内容
                        esc.addStoreQRCodeData(content);
                        // 打印QRCode
                        esc.addPrintQRCode();
                  }else if("image".equals(type)){
                        byte[] bytes = Base64.decode(content, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        esc.addRastBitImage(bitmap, 576, 0);
                  }

                  if(linefeed == 1){
                        //打印并换行
                        esc.addPrintAndLineFeed();
                  }

            }

            //打印走纸n个单位
            //esc.addPrintAndFeedLines((byte) 4);

            // 开钱箱
            esc.addGeneratePlus(LabelCommand.FOOT.F2, (byte) 255, (byte) 255);
            //开启切刀
            esc.addCutPaper();
            //添加缓冲区打印完成查询
            byte [] bytes={0x1D,0x72,0x01};
            //添加用户指令
            esc.addUserCommand(bytes);

            return esc.getCommand();
      }

      /**
       * 标签打印对象转换
       * @return
       */
      public static Vector<Byte> mapToLabel(Map<String,Object> config, List<Map<String,Object>> list) {
            LabelCommand tsc = new LabelCommand();

            int width = (int)(config.get("width")==null?60:config.get("width")); // 单位：mm
            int height = (int)(config.get("height")==null?40:config.get("height")); // 单位：mm
            int gap = (int)(config.get("gap")==null?0:config.get("gap")); // 单位：mm

           
            tsc.addSize(width, height);
            
            tsc.addGap(gap);
            
            tsc.addDirection(LabelCommand.DIRECTION.FORWARD, LabelCommand.MIRROR.NORMAL);
            
            tsc.addQueryPrinterStatus(LabelCommand.RESPONSE_MODE.ON);
           
            tsc.addReference(0, 0);
            
            tsc.addDensity(LabelCommand.DENSITY.DNESITY4);
            
            tsc.addTear(EscCommand.ENABLE.ON);
            
            tsc.addCls();

            // {type:'text|barcode|qrcode|image', content:'', x:0,y:0}
            for (Map<String,Object> m: list) {
                  String type = (String)m.get("type");
                  String content = (String)m.get("content");
                  int x = (int)(m.get("x")==null?0:m.get("x")); //dpi: 1mm约为8个点
                  int y = (int)(m.get("y")==null?0:m.get("y"));

                  if("text".equals(type)){
                        
                        tsc.addText(x, y, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1, content);
                        
                  }else if("barcode".equals(type)){
                        tsc.add1DBarcode(x, y, LabelCommand.BARCODETYPE.CODE128, 100, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, "SMARNET");
                  }else if("qrcode".equals(type)){
                        tsc.addQRCode(x,y, LabelCommand.EEC.LEVEL_L, 5, LabelCommand.ROTATION.ROTATION_0, content);
                  }else if("image".equals(type)){
                        byte[] bytes = Base64.decode(content, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        tsc.addBitmap(x, y, LabelCommand.BITMAP_MODE.OVERWRITE, 300, bitmap);
                  }
            }

            
            tsc.addPrint(1, 1);
            
            tsc.addSound(2, 100);
            
            tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
            
            return  tsc.getCommand();
      }

      /**
       * 面单打印对象转换
       * @return
       */
      public static Vector<Byte> mapToCPCL(Map<String,Object> config, List<Map<String,Object>> list) {
            CpclCommand cpcl = new CpclCommand();


            Vector<Byte> datas = cpcl.getCommand();
            return datas;
      }

}
