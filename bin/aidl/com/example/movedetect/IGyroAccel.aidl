package com.example.movedetect;

interface IGyroAccel {
  oneway void sampleCounter( in int count );
  oneway void statusMessage( in int state );
  oneway void diff( in double x, in double y, in double z );
}
