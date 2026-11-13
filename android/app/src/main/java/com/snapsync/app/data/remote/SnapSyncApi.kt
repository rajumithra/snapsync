package com.snapsync.app.data.remote

import com.snapsync.app.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface SnapSyncApi {

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<AuthResponseDto>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponseDto>

    @GET("auth/me")
    suspend fun getMe(): Response<UserDto>

    @GET("events")
    suspend fun getEvents(): Response<List<EventDto>>

    @POST("events")
    suspend fun createEvent(@Body request: CreateEventRequest): Response<EventDto>

    @GET("events/{id}")
    suspend fun getEventById(@Path("id") id: String): Response<EventDto>

    @POST("photos/upload-url")
    suspend fun requestUploadUrl(@Body request: PhotoUploadUrlRequestDto): Response<PhotoUploadUrlResponseDto>

    @POST("photos/confirm-upload")
    suspend fun confirmUpload(@Body request: ConfirmUploadRequestDto): Response<PhotoResponseDto>

    @Multipart
    @POST("photos/direct-upload")
    suspend fun directUpload(
        @Query("photo_id") photoId: String,
        @Query("target") target: String,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    @GET("photos/event/{eventId}")
    suspend fun getEventPhotos(
        @Path("eventId") eventId: String,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50
    ): Response<List<PhotoResponseDto>>

    @GET("admin/dashboard-stats")
    suspend fun getDashboardStats(): Response<DashboardStatsDto>
}

