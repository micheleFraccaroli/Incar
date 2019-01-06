package com.example.michele.incar;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface CoordsDAO {
    @Query("SELECT * FROM Coords ORDER BY ucoords DESC LIMIT 2")
    List<Coords> getCoords();

    @Insert
    void insertCoords(Coords c);
}
