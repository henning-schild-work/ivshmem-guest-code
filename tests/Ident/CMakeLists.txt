cmake_minimum_required(VERSION 2.6)
project(campino)

add_executable(getident getident)
add_library(ivshmem ivshmem)

set(CMAKE_C_FLAGS_DEBUG "-DDEBUG")
set(CMAKE_C_FLAGS "-std=c99 -g -Wall")
target_link_libraries(getident ivshmem rt crypto)

