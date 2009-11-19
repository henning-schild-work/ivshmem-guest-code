package org.ualberta.shm;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Runnable;

private class ShmServer extends Shm implements Runnable {
    public static void main(String args[]) throws Exception {
        String devname = args[0];
        int msize = Integer.parseInt(args[1]);
        int nblocks = Integer.parseInt(args[2]);
        int nchunks = Integer.parseInt(args[3]);

        ShmServer s = new ShmServer(devname, msize, nblocks, nchunks);
        s.run();
    }

    public ShmServer(String devname, int msize, int nblocks, int nchunks, int nmachines) throws IOException {
        super(devname, msize, nblocks, nchunks, nmachines);
    }

    public void run() {
        try {
            doRun();
        } catch(Exception e) {
            return;
        }
    }

    private void doRun() throws IOException {
        int me;
        int block;
        String sendfile;
        boolean quit = false;
        byte[] bytes = new byte[64*1024];

        /* What is my VM number? */
        me = _mem.getPosition();
        _mem.writeInt(-1, SYNC(me) + BLK);
        LOG.info("[SHM] I am VM number " + String.valueOf(me));

        while(!quit) {

            /* Wait for a client */
            LOG.info("[SHM] Waiting for a client.");
            do {
                block = _mem.readInt(SYNC(me) + BLK);
            } while(block == -1);

            LOG.info("[SHM] Got a client with block " + String.valueOf(block));

            /* Read the filename */
            sendfile = _mem.readString(BASE(block) + FNAME);
            LOG.info("[SHM] Client sent filename: " + sendfile);

            Hashtable<String, String[]> query = HttpUtils.parseQueryString(sendfile);
            String jobId = query.get("job")[0];
            int reduce = Integer.parseInt(query.get("reduce")[0]);
            StringTokenizer itr = new StringTokenizer(query.get("map")[0], ",");

            JobConf conf = (JobConf)server.getAttribute("conf");
            LocalDirAllocator lDirAlloc = (LocalDirAllocator)server.getAttribute("localDirAllocator");
            FileSystem rfs = ((LocalFileSystem)server.getAttribute("local.file.system")).getRaw();
            TaskTracker tracker = (TaskTracker)server.getAttribute("task.tracker");

            String userName = null;
            synchronized (tracker.runningJobs) {
                RunningJob rjob = tracker.runningJobs.get(JobID.forName(jobId));
                if (rjob == null) {
                    throw new IOException("Unknown job " + jobId + "!!");
                }
                userName = rjob.jobConf.getUser();
            }

            ShmOutputStream shmout = new ShmOutputStream(this, block);
            DataOutputStream dataout = new DataOutputStream(shmout);

            itr = new StringTokenizer(query.get("map")[0], ",");
            while(itr.hasMoreTokens()) {
                String mapId = itr.nextToken();
                
                LOG.info("[SHM] Reading map output " + mapId);

                Path mapOutputFileName = lDirAlloc.getLocalPathToRead(TaskTracker.getIntermediateOutputDir( userName, jobId, mapId) + "/file.out", conf);
                Path indexFileName = lDirAlloc.getLocalPathToRead(TaskTracker.getIntermediateOutputDir( userName, jobId, mapId) + "/file.out.index", conf);
                IndexRecord info = tracker.indexCache.getIndexInformation(mapId, reduce, indexFileName);
                FSDataInputStream mapOutputIn = rfs.open(mapOutputFileName);
                mapOutputIn.seek(info.startOffset);

                ShuffleHeader hdr = new ShuffleHeader(mapId, info.partLength, info.rawLength, reduce);
                hdr.write(dataout);

                long rem = info.partLength;
                int len = mapOutputIn.read(bytes, 0, (int)Math.min(rem, CHUNK_SZ));
                long now = 0;
                while (len >= 0) {
                    rem -= len;
                    if (len > 0) {
                        shmout.write(bytes, 0, len);
                    } else {
                        LOG.info("Skipped zero-length read of map " + mapId + 
                                " to reduce " + reduce);
                    }
                    if (rem == 0) {
                        break;
                    }
                    len = mapOutputIn.read(bytes, 0, (int)Math.min(rem, CHUNK_SZ));
                }

                mapOutputIn.close();
            }

            _mem.writeInt(-1, SYNC(me) + BLK);
            dataout.close();
            shmout.close();
            LOG.info("[SHM] Done sending map outputs.");
        }

        _mem.closeDevice();
    }
  }
}
