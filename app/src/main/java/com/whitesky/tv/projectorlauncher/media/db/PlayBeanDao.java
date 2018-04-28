package com.whitesky.tv.projectorlauncher.media.db;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

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

    // 使用一个list循环删除,需要注意每删除一个就会改变数据库中下一个的idx,而与list中的不匹配
    public void delete(PlayBean data) {
        delete(data.getIdx());
    }

    // 使用一个list循环删除,需要注意每删除一个就会改变数据库中下一个的idx,而与list中的不匹配
    public void delete(int idx) {
        synchronized (mapLock) {
            try {
                dao.deleteById(idx);

                // 后面条目的idx顺次减1
                List<PlayBean> needUpdateBeans = selectIdxLargerAll(idx);
                if (needUpdateBeans!=null) {
                    for (PlayBean bean:needUpdateBeans) {
                        dao.updateId(bean,bean.getIdx()-1);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 删除media表中的所有数据
    public void deleteAll() {
        synchronized (mapLock) {
            delete(selectAll());
        }
    }

    public void delete(List<PlayBean> datas) {
        if (datas==null | datas.isEmpty()) {
            return;
        }

        synchronized (mapLock) {
            Collections.sort(datas);
            int deleteCount = 0;
            for (PlayBean bean:datas) {
                delete(bean.getIdx() - deleteCount);
                deleteCount++;
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

    private List<PlayBean> selectIdxLargerAll(int idx) {
        synchronized (mapLock) {
            List<PlayBean> items = null;
            try {
                items = dao.queryBuilder().where()
                        .gt(PlayBean.COLUMNNAME_IDX, idx)
                        .query();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return items;
        }
    }
}
