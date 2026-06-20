package com.example.streamer.di

import android.content.Context
import com.example.streamer.data.StreamRepositoryImpl
import com.example.streamer.domain.StreamRepository
import com.example.streamer.encoder.H264Encoder
import com.example.streamer.streaming.RtspServer
import com.example.streamer.streaming.SocketFrameSender
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindings {
    @Binds
    abstract fun bindStreamRepository(impl: StreamRepositoryImpl): StreamRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideSocketFrameSender(): SocketFrameSender = SocketFrameSender()

    @Provides
    @Singleton
    fun provideEncoder(): H264Encoder = H264Encoder(width = 1280, height = 720, fps = 30, bitRate = 2_500_000)

    @Provides
    @Singleton
    fun provideRtspServer(@ApplicationContext context: Context): RtspServer = RtspServer(context, port = 8554)
}

