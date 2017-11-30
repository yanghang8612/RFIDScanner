package com.casc.rfidscanner.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.casc.rfidscanner.bean.Tag;
import com.casc.rfidscanner.helper.DBOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class TagDao extends BaseDao {
    public TagDao(Context context) {
        super(DBOpenHelper.TABLE_NAME_TAG, context);
    }

    /**
     * 插入新Tag
     *
     * @param tag
     */
    public long save(Tag tag) {
        ContentValues values = tag.transfor2ContentValues();
        return insertContentValues(values);
    }

    /**
     * 查找所有Tag
     *
     * @return
     */
    public List<Tag> find() {
        List<Tag> result = new ArrayList<Tag>();
        Cursor c = simpleQuery(); // remain

        while (c.moveToNext()) {
            result.add(Tag.transforByCursor(c));
        }
        c.close();
        return result;
    }

    /**
     * 按照id删除Tag
     *
     * @param id
     */
    public int delete(int id) {
        return deleteByid(id);
    }

    /**
     * 删除Tag
     *
     * @param tag
     */
    public int delete(Tag tag) {
        return deleteByid(tag.getId());
    }

    /**
     * 修改Tag
     *
     * @param tag
     * @return
     */
    public int update(Tag tag) {
        ContentValues values = tag.transfor2ContentValues();
        return update(values, "_id=?", new String[]{tag.getId().toString()});
    }

    /**
     * 通过id查找Tag
     *
     * @param id
     * @return
     */
    public Tag getById(int id) {
        Cursor c = getByIdCursor(id);
        while (c.moveToNext()) {
            Tag tag = Tag.transforByCursor(c);
            c.close();
            return tag;
        }
        return null;
    }
}
