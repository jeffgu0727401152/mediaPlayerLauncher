package com.whitesky.tv.projectorlauncher.media.db;

import android.content.Context;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.List;

import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_DOWNLOADED;

/**
 * Created by jeff on 18-2-4.
 */

public class PlayBeanDao {
    private Context context;
    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<PlayBean, Integer> dao;
    static final Object mapLock = new Object();

    public PlayBeanDao(Context context) {
        this.context = context;
        try {
            this.dao = DatabaseHelper.getInstance(context).getDao(PlayBean.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createOrUpdate(PlayBean data) {
        synchronized (mapLock) {
            try {
                dao.createOrUpdate(data);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void delete(PlayBean data) {
        synchronized (mapLock) {
            try {
                dao.delete(data);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void update(PlayBean data) {
        synchronized (mapLock) {
            try {
                dao.update(data);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void createOrUpdate(List<PlayBean> datas) {
        synchronized (mapLock) {
            for (PlayBean itemInsert : datas) {
                createOrUpdate(itemInsert);
            }
        }
    }

    public void delete(List<PlayBean> datas) {
        synchronized (mapLock) {
            try {
                dao.delete(datas);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    public List<PlayBean> selectAll() {
        synchronized (mapLock) {
            List<PlayBean> users = null;
            try {
                users = dao.queryForAll();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return users;
        }
    }

    // 删除media表中的所有数据
    public void deleteAll() {
        synchronized (mapLock) {
            delete(selectAll());
        }
    }

    // 根据主键取出用户信息,这里的主键是path
    public PlayBean queryByIdx(int idx) {
        synchronized (mapLock) {
            PlayBean user = null;
            try {
                user = dao.queryForId(idx);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return user;
        }
    }
}
