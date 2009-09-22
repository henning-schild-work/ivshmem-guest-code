cmake_minimum_required(VERSION 2.6)
project(campino)

add_executable(test1 sendioctl)
add_executable(sum_sema sum_sema)
add_executable(dump_sema dump_sema)
add_library(ivshmem ivshmem)

set(CMAKE_C_FLAGS_DEBUG "-DDEBUG")
set(CMAKE_C_FLAGS "-std=c99 -g -Wall")
target_link_libraries(test1 ivshmem rt crypto)
target_link_libraries(sum_sema ivshmem rt crypto)
target_link_libraries(dump_sema ivshmem rt crypto pthread)

