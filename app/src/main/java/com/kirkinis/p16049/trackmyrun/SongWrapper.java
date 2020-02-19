package com.kirkinis.p16049.trackmyrun;

import java.io.Serializable;
import java.util.ArrayList;

public class SongWrapper implements Serializable {

    private static final long serialVersionUID = 1L;
    private ArrayList<Song> itemDetails;

    public SongWrapper(ArrayList<Song> items) {
        this.itemDetails = items;
    }

    public ArrayList<Song> getItemDetails() {
        return itemDetails;
    }
}