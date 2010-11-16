cmake_minimum_required(VERSION 2.6)
project(nahanni)

add_executable(uio_send uio_send)
add_executable(uio_read uio_read)
add_executable(sum_sema sum_sema)
add_executable(dump_sema dump_sema)
add_executable(getident getident)
add_executable(recv_file recv_file)
add_library(ivshmem ivshmem)

set(CMAKE_C_FLAGS_DEBUG "-DDEBUG")
set(CMAKE_C_FLAGS "-std=c99 -g -Wall -D_BSD_SOURCE")

target_link_libraries(getident ivshmem rt crypto)
target_link_libraries(recv_file ivshmem rt crypto m)
target_link_libraries(sum_sema ivshmem rt crypto)
target_link_libraries(dump_sema ivshmem rt crypto pthread)

