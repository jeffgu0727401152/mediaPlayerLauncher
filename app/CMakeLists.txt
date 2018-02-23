cmake_minimum_required(VERSION 3.4.1)

include_directories(
    ${CMAKE_SOURCE_DIR}/cpp/main/jni        #h文件目录
)

#=================添加需要编译的库(开始)==============================================================
add_library(
    PolygonMask                          #希望so库被编译出来的名称，java代码loadlibrary用这个名称
    SHARED
    src/main/cpp/polygon_native_lib.cpp     #cpp代码文件的路径 从这里往下可以随意添加c、c++文件
    src/main/cpp/PolygonBuffer.cpp
)

add_library(
    AtcCert
    SHARED
    src/main/cpp/atc_native_lib.cpp
)
#=================添加需要编译的库(结束)==============================================================




#=================添加需要的链接库(开始)===================================================
find_library(       #定位NDK中的库,需传入两个参数(path变量名、ndk库名称)
    log-lib         # 设置找到的path变量的别名为log-lib
    log             # 指定cmake查询库的名称,即在ndk开发包中查询liblog.so函数库，将其路径赋值给log-lib
)
#=================添加需要的链接库(结束)===================================================




#=================链接规则(开始)========================================
target_link_libraries(
    PolygonMask
    ${log-lib}
)

target_link_libraries(
    AtcCert
    ${log-lib}
)
#=================链接规则(结束)========================================