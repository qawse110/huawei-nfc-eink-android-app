package org.nfcresearch.qpolllab;
import java.io.*;

/** Evidence is read from firmware configuration files; it never identifies the chip from an ID. */
public final class ChipInfo {
    private static final String[] PATHS={"/odm/etc/libnfc-nxp.conf","/vendor/etc/libnfc-nxp.conf",
            "/odm/etc/libnfc-nci.conf","/vendor/etc/libnfc-nci.conf"};
    /** Oplus/realme keep per-process copies such as libnfc-mtp-SN220.conf_<pid> under /odm/etc/nfc. */
    static final File EXTRA_DIRECTORY=new File("/odm/etc/nfc");
    private ChipInfo(){}
    public static String inspect(){
        for(String path:PATHS){
            String result=from(new File(path));
            if(result!=null)return result;
        }
        // /odm is erofs (read-only) on those ROMs, so the standard paths above never carry the
        // evidence; scan the directory copies in a stable order instead of guessing file names.
        File[] extra=EXTRA_DIRECTORY.listFiles();
        if(extra!=null){
            java.util.Arrays.sort(extra);
            for(File file:extra){
                if(!file.isFile()||!isEvidenceCopy(file.getName()))continue;
                String result=from(file);
                if(result!=null)return result;
            }
        }
        return "未能从当前可读配置识别，请查看系统日志；不据此判定不支持 NFC 或 Q-Poll。";
    }
    /** True for the vendor firmware copies this layout installs, never for unrelated files. */
    static boolean isEvidenceCopy(String name){
        if(name==null||!name.startsWith("libnfc-mtp-"))return false;
        int marker=name.indexOf(".conf");
        if(marker<0)return false;
        String suffix=name.substring(marker+".conf".length());
        // The base copy, or the per-process copy the ROM installs as _<pid>; nothing else.
        return suffix.isEmpty()||suffix.matches("_\\d+");
    }
    private static String from(File file){
        try(BufferedReader in=new BufferedReader(new FileReader(file))){
            String line;StringBuilder text=new StringBuilder();
            while((line=in.readLine())!=null&&text.length()<262144)text.append(line).append('\n');
            return NxpHalPolicy.firmwareConfig(text.toString(),file.getPath());
        }catch(IOException ignored){return null;}
    }
}
