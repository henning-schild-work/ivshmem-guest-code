#ifndef _FTP_H
#define _FTP_H

#define CHUNK_SZ  (16*1024*1024)
#define NEXT(i)   ((i + 1) % 15)
#define OFFSET(i) (i * CHUNK_SZ)

#define FULL_LOC  memptr
#define EMPTY_LOC (memptr + sizeof(sem_t))
#define BUF_LOC   (memptr + CHUNK_SZ)

#endif /* _FTP_H */
