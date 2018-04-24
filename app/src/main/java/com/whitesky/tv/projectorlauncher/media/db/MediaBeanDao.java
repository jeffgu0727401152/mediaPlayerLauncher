package com.whitesky.tv.projectorlauncher.media.db;

import android.content.Context;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.List;

import static com.whitesky.tv.projectorlauncher.media.db.MediaBean.STATE_DOWNLOAD_DOWNLOADED;

/**
 * Created by jeff on 18-2-4.
 */

public class MediaBeanDao {
    private Context context;
    // ORMLite提供的DAO类对象，第一个泛型是要操作的数据表映射成的实体类；第二个泛型是这个实体类中ID的数据类型
    private Dao<MediaBean, String> dao;
    static final Object mediaDbLock = new Object();

    public MediaBeanDao(Context context) {
        this.context = context;
        try {
            this.dao = DatabaseHelper.getInstance(context).getDao(MediaBean.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 向media表中添加一条数据
    public void createOrUpdate(MediaBean data) {
        synchronized (mediaDbLock) {
            try {
                dao.createOrUpdate(data);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 删除media表中的一条数据
    public void delete(MediaBean data) {
        synchronized (mediaDbLock) {
            try {
                dao.delete(data);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 修改media表中的一条数据
    public void update(MediaBean data) {
        synchronized (mediaDbLock) {
            try {
                dao.update(data);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 向media表中添加多条数据
    public void createOrUpdate(List<MediaBean> datas) {
        synchronized (mediaDbLock) {
            for (MediaBean itemInsert : datas) {
                createOrUpdate(itemInsert);
            }
        }
    }

    // 删除media表中的多条数据
    public void delete(List<MediaBean> datas) {
        synchronized (mediaDbLock) {
            try {
                dao.delete(datas);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 查询出表中的所有数据
    public List<MediaBean> selectAll() {
        synchronized (mediaDbLock) {
            List<MediaBean> users = null;
            try {
                users = dao.queryForAll();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return users;
        }
    }

    // 按照显示名查询出表中的所有数据
    public List<MediaBean> selectAllByNameOrder(boolean ascending) {
        synchronized (mediaDbLock) {
            List<MediaBean> items = null;
            try {
                items = dao.queryBuilder().orderBy(MediaBean.COLUMNNAME_ORDER_DESCRIPTION, ascending).query();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return items;
        }
    }

    // 按照播放时长查询出表中的所有数据
    public List<MediaBean> selectAllByDurationOrder(boolean ascending) {
        synchronized (mediaDbLock) {
            List<MediaBean> items = null;
            try {
                items = dao.queryBuilder().orderBy(MediaBean.COLUMNNAME_DURATION, ascending).query();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return items;
        }
    }

    // 按照文件类型查询出表中的所有数据
    public List<MediaBean> selectAllBySourceOrder(boolean ascending) {
        {
            List<MediaBean> items = null;
            try {
                items = dao.queryBuilder().orderBy(MediaBean.COLUMNNAME_SOURCE, ascending).query();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return items;
        }
    }

    // 删除media表中的所有数据
    public void deleteAll() {
        synchronized (mediaDbLock) {
            delete(selectAll());
        }
    }

    // 查询出表中的所有需要继续下载的媒体记录
    public List<MediaBean> selectItemsDownloading() {
        synchronized (mediaDbLock) {
            List<MediaBean> retList = null;
            try {
                retList = dao.queryBuilder().where()
                        .eq(MediaBean.COLUMNNAME_DOWNLOAD_STATE, MediaBean.STATE_DOWNLOAD_DOWNLOADING)
                        .or().eq(MediaBean.COLUMNNAME_DOWNLOAD_STATE, MediaBean.STATE_DOWNLOAD_WAITING)
                        .or().eq(MediaBean.COLUMNNAME_DOWNLOAD_STATE, MediaBean.STATE_DOWNLOAD_START)
                        .or().eq(MediaBean.COLUMNNAME_DOWNLOAD_STATE, MediaBean.STATE_DOWNLOAD_PAUSED)
                        .or().eq(MediaBean.COLUMNNAME_DOWNLOAD_STATE, MediaBean.STATE_DOWNLOAD_ERROR)
                        .query();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return retList;
        }
    }

    // 查询出表中的所有通过U盘导入到本地的媒体记录
    public List<MediaBean> selectItemsLocalImport() {
        synchronized (mediaDbLock) {
            List<MediaBean> retList = null;
            try {
                retList = dao.queryBuilder().where()
                        .eq(MediaBean.COLUMNNAME_SOURCE, MediaBean.SOURCE_LOCAL)
                        .query();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return retList;
        }
    }

    // 查询出所有云端下载到本地的文件
    public List<MediaBean> selectDownloadedItemsFromCloud() {
        synchronized (mediaDbLock) {
            List<MediaBean> retList = null;
            try {
                retList = dao.queryBuilder().where()
                        .gt(MediaBean.COLUMNNAME_SOURCE, MediaBean.SOURCE_LOCAL)
                        .and().eq(MediaBean.COLUMNNAME_DOWNLOAD_STATE, STATE_DOWNLOAD_DOWNLOADED)
                        .query();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return retList;
        }
    }


    // 删除media表中本地USB导入的数据
    public void deleteItemsLocalImport() {
        synchronized (mediaDbLock) {
            try {
                List<MediaBean> delList = dao.queryBuilder().where()
                        .eq(MediaBean.COLUMNNAME_SOURCE, MediaBean.SOURCE_LOCAL)
                        .query();
                dao.delete(delList);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 删除media表中所有云端的数据
    public void deleteItemsFromCloud() {
        synchronized (mediaDbLock) {
            try {
                List<MediaBean> delList = dao.queryBuilder().where()
                        .gt(MediaBean.COLUMNNAME_SOURCE, MediaBean.SOURCE_LOCAL)
                        .query();
                dao.delete(delList);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 根据主键取出用户信息,这里的主键是path
    public MediaBean queryByPath(String path) {
        synchronized (mediaDbLock) {
            if (path == null) {
                return null;
            }

            MediaBean user = null;
            try {
                user = dao.queryForId(path);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return user;
        }
    }

    // 根据ID取出用户信息,非主键可能取出多个
    public List<MediaBean> queryById(int id) {
        synchronized (mediaDbLock) {
            List<MediaBean> retList = null;
            try {
                retList = dao.queryBuilder().where()
                        .eq(MediaBean.COLUMNNAME_ID, id).query();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return retList;
        }
    }

    public List<MediaBean> queryByUrl(String url) {
        synchronized (mediaDbLock) {
            List<MediaBean> retList = null;
            try {
                retList = dao.queryBuilder().where()
                        .eq(MediaBean.COLUMNNAME_URL, url).query();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return retList;
        }
    }

}
