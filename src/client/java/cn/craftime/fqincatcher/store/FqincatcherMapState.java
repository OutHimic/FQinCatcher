package cn.craftime.fqincatcher.store;

import java.util.ArrayList;
import java.util.List;

public final class FqincatcherMapState {
    public int version = 1;
    public String selectedMapId;
    public List<FqincatcherMapProfile> maps = new ArrayList<>();

    public FqincatcherMapState() {
    }
}

