package com.example.androidmap.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.androidmap.model.SearchHistory;
import java.util.List;

@Dao
public interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SearchHistory history);

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 20")
    List<SearchHistory> getAllHistory();

    @Query("DELETE FROM search_history")
    void deleteAll();

    @Query("DELETE FROM search_history WHERE placeName = :name")
    void deleteByName(String name);
}
