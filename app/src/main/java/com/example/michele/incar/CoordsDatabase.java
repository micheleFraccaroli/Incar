package com.example.michele.incar;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {Coords.class}, version = 1)
public abstract class CoordsDatabase extends RoomDatabase {
    public abstract CoordsDAO coordsDAO();
}
