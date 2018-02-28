package com.whitesky.tv.projectorlauncher.media.db;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by jeff on 18-2-4.
 */

public class MediaBeanDao {
    private Context context;
    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<MediaBean, String> dao;

    public MediaBeanDao(Context context) {
        this.context = context;
        try {
            this.dao = MediaDatabaseHelper.getInstance(context).getDao(MediaBean.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 向media表中添加一条数据
    public void insert(MediaBean data) {
        try {
            dao.createOrUpdate(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 删除media表中的一条数据
    public void delete(MediaBean data) {
        try {
            dao.delete(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 修改media表中的一条数据
    public void update(MediaBean data) {
        try {
            dao.update(data);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 查询media表中的所有数据
    public List<MediaBean> selectAll() {
        List<MediaBean> users = null;
        try {
            users = dao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // 删除media表中的所有数据
    public void deleteAll() {
        try {
            TableUtils.clearTable(MediaDatabaseHelper.getInstance(context).getConnectionSource(), MediaBean.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // 根据ID取出用户信息
    public MediaBean queryById(String id) {
        MediaBean user = null;
        try {
            user = dao.queryForId(id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }
}
