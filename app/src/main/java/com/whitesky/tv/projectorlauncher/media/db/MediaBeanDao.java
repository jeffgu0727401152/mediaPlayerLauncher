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

    // 向media表中添加多条数据
    public void insert(List<MediaBean> datas) {
        for (MediaBean itemInsert:datas) {
            insert(itemInsert);
        }
    }

    // 删除media表中的多条数据
    public void delete(List<MediaBean> datas) {
        try {
            dao.delete(datas);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 查询出表中的所有数据
    public List<MediaBean> selectAll() {
        List<MediaBean> users = null;
        try {
            users = dao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // 按照显示名查询出表中的所有数据
    public List<MediaBean> selectAllByNameOrder(boolean ascending) {
        List<MediaBean> items = null;
        try {
            items = dao.queryBuilder().orderBy(MediaBean.COLUMNNAME_ORDER_DESCRIPTION,ascending).query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    // 按照播放时长查询出表中的所有数据
    public List<MediaBean> selectAllByDurationOrder(boolean ascending) {
        List<MediaBean> items = null;
        try {
            items = dao.queryBuilder().orderBy(MediaBean.COLUMNNAME_DURATION,ascending).query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    // 按照文件类型查询出表中的所有数据
    public List<MediaBean> selectAllBySourceOrder(boolean ascending) {
        List<MediaBean> items = null;
        try {
            items = dao.queryBuilder().orderBy(MediaBean.COLUMNNAME_SOURCE,ascending).query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    // 删除media表中的所有数据
    public void deleteAll() {
        delete(selectAll());
    }

    // 查询出表中的所有通过U盘导入到本地的媒体记录
    public List<MediaBean> selectItemsLocalImport() {
        List<MediaBean> retList = null;
        try {
            retList = dao.queryBuilder().where().eq(MediaBean.COLUMNNAME_SOURCE,MediaBean.SOURCE_LOCAL).query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return retList;
    }


    // 删除media表中本地USB导入的数据
    public void deleteItemsLocalImport() {
        try {
            List<MediaBean> delList = dao.queryBuilder().where().eq(MediaBean.COLUMNNAME_SOURCE,MediaBean.SOURCE_LOCAL).query();
            dao.delete(delList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 删除media表中所有云端的数据
    public void deleteItemsFromCloud() {
        try {
            List<MediaBean> delList = dao.queryBuilder().where().gt(MediaBean.COLUMNNAME_SOURCE,MediaBean.SOURCE_LOCAL).query();
            dao.delete(delList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 根据主键取出用户信息,这里的id是path
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
