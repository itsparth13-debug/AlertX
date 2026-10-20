package com.yourname.safereport.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Room Data Access Object for draft incidents.
 */
@Dao
public interface DraftIncidentDao {

    @Insert
    long insertDraft(DraftIncident draft);

    @Query("SELECT * FROM draft_incidents ORDER BY createdAt DESC")
    List<DraftIncident> getAllDrafts();

    @Delete
    void deleteDraft(DraftIncident draft);

    @Query("DELETE FROM draft_incidents WHERE id = :id")
    void deleteDraftById(long id);
}
