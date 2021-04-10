package com.bm.messenger.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Objects;

@Entity(tableName = "user")
public class UserModel implements Serializable {

    @ColumnInfo(name = "user_id")
    @PrimaryKey
    @NonNull
    public String id;
    @ColumnInfo(name = "user_name")
    @NonNull
    public String name;

    public UserModel(@NonNull String id, @NonNull String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
//        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserModel userModel = (UserModel) o;
        return id.equals(userModel.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id + "$" + name;
    }
}
