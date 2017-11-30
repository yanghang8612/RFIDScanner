package com.casc.rfidscanner.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.casc.rfidscanner.bean.User;
import com.casc.rfidscanner.helper.DBOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class UserDao extends BaseDao {
    public UserDao(Context context) {
        super(DBOpenHelper.TABLE_NAME_USER, context);
    }


    /**
     * 插入新User
     *
     * @param user
     */
    public long save(User user) {
        ContentValues values = user.transfor2ContentValues();
        return insertContentValues(values);
    }

    /**
     * 查找所有User
     *
     * @return
     */
    public List<User> find() {
        List<User> result = new ArrayList<User>();
        Cursor c = simpleQuery(); // remain

        while (c.moveToNext()) {
            result.add(User.transforByCursor(c));
        }
        return result;
    }

    /**
     * 按照id删除User
     *
     * @param id
     */
    public int delete(int id) {
        return deleteByid(id);
    }

    /**
     * 删除User
     *
     * @param user
     */
    public int delete(User user) {
        return deleteByid(user.getId());
    }

    /**
     * 修改User
     *
     * @param user
     * @return
     */
    public int update(User user) {
        ContentValues values = user.transfor2ContentValues();
        return update(values, "_id=?", new String[]{user.getId().toString()});
    }

    /**
     * 通过id查找User
     *
     * @param id
     * @return
     */
    public User getById(int id) {
        Cursor c = getByIdCursor(id);
        return User.transforByCursor(c);
    }
}
