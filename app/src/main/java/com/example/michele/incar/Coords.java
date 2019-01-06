package com.example.michele.incar;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;


@Entity
public class Coords {
    @PrimaryKey
    public int ucoords;

    @ColumnInfo(name = "longitGPS")
    public String longitGPS;

    @ColumnInfo(name = "latidGPS")
    public String latidGPS;
}