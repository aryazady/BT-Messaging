//package com.bm.messenger.model;
//
//import androidx.annotation.NonNull;
//import androidx.room.ColumnInfo;
//import androidx.room.Entity;
//import androidx.room.ForeignKey;
//
//import static androidx.room.ForeignKey.CASCADE;
//
//@Entity(tableName = "user_message",
//        primaryKeys = {"user_id", "message_id"},
//        foreignKeys = {
//                @ForeignKey(entity = UserModel.class,
//                        parentColumns = "user_id",
//                        childColumns = "user_id",
//                        onDelete = CASCADE),
//                @ForeignKey(entity = MessageModel.class,
//                        parentColumns = "message_id",
//                        childColumns = "message_id",
//                        onDelete = CASCADE)
//        })
//public class UserMessageJoin {
//
//    @ColumnInfo(name = "user_id", index = true)
//    @NonNull
//    public String userId;
//    @ColumnInfo(name = "message_id", index = true)
//    @NonNull
//    public String messageId;
//
//    public UserMessageJoin(@NonNull String userId, @NonNull String messageId) {
//        this.userId = userId;
//        this.messageId = messageId;
//    }
//}
