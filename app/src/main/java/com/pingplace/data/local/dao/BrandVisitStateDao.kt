package com.pingplace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pingplace.data.local.entity.BrandVisitStateEntity

@Dao
interface BrandVisitStateDao {

    @Query("SELECT * FROM brand_visit_state WHERE brandQuery = :brandQuery")
    suspend fun getByBrand(brandQuery: String): BrandVisitStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(state: BrandVisitStateEntity)
}
