package net.dirtcraft.dirtlauncher.game.installation.tasks.download;

import java.util.Arrays;
import java.util.Iterator;

public enum  DataRates {
    BYTES       ((long) Math.pow(1024, 0), "B"),
    KILOBYTES   ((long) Math.pow(1024, 1), "KB"),
    MEGABYTES   ((long) Math.pow(1024, 2), "MB"),
    GIGABYTES   ((long) Math.pow(1024, 3), "GB"),
    TERABYTES   ((long) Math.pow(1024, 4), "TB"),
    PETABYTES   ((long) Math.pow(1024, 5), "PB"),
    EXABYTES    ((long) Math.pow(1024, 6), "EB"),
    ZETTABYTES  ((long) Math.pow(1024, 7), "ZB"),
    YOTTABYTES  ((long) Math.pow(1024, 8), "YB");
    private final long bytes;
    private final String suffix;
    DataRates(long i, String suffix){
        this.bytes = i;
        this.suffix = suffix;
    }

    public static DataRates getMaximumDataRate(long bytes){
        Iterator<DataRates> rates = Arrays.asList(DataRates.values()).iterator();
        DataRates previousRate = DataRates.BYTES;
        DataRates currentRate;
        while (rates.hasNext() && bytes >= (currentRate = rates.next()).getBytes()) previousRate = currentRate;
        return previousRate;
    }

    public static String getBitrate(long bytes){
        DataRates rates = getMaximumDataRate(bytes);
        return rates.toBitrate(bytes);
    }

    public static String getFileSize(long bytes){
        DataRates rates = getMaximumDataRate(bytes);
        return rates.toFileSize(bytes);
    }

    public String toBitrate(long i){
        if (this == BYTES) return i + getSuffix();
        return String.format("%.1f%s/s", (double) i / getBytes(), getSuffix());
    }

    public String toFileSize(long i){
        if (this == BYTES) return i + getSuffix();
        return String.format("%.1f%s", (double) i / getBytes(), getSuffix());
    }

    public long getBytes(){
        return bytes;
    }

    public String getSuffix(){
        return suffix;
    }
}
