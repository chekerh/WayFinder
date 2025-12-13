package tn.esprit.wayfinder.network

import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import retrofit2.http.*
import tn.esprit.wayfinder.models.*

interface ApiService {

    // --- AUTH --- //
    @POST("auth/register")
    suspend fun register(@Body request: SignUpRequest): SignUpResponse

    @POST("auth/send-otp-for-registration")
    suspend fun sendOTPForRegistration(@Body request: SendOTPForRegistrationRequest): SendOTPForRegistrationResponse

    @POST("auth/register-with-otp")
    suspend fun registerWithOTP(@Body request: RegisterWithOTPRequest): RegisterWithOTPResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/google")
    suspend fun googleSignIn(@Body request: GoogleSignInRequest): GoogleSignInResponse

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): VerifyEmailResponse

    @GET("auth/verify-email")
    suspend fun verifyEmailGet(@Query("token") token: String): VerifyEmailResponse

    @POST("auth/resend-verification")
    suspend fun resendVerificationEmail(@Body request: ResendVerificationRequest): ResendVerificationResponse

    // --- USER --- //
    @GET("user/profile")
    suspend fun getProfile(): User

    @PUT("user/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): User

    @Multipart
    @POST("user/profile/upload-image")
    suspend fun uploadProfileImage(@Part image: MultipartBody.Part): UploadProfileImageResponse

    @POST("user/fcm-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): FcmTokenResponse

    // --- BOOKING --- //
    @GET("booking/offers")
    suspend fun getOffers(
        @Query("destination") destination: String?,
        @Query("dates") dates: String?,
        @Query("type") type: String?
    ): List<Offer>

    @GET("booking/compare")
    suspend fun getOfferComparison(@Query("offer_id") offerId: String): OfferComparison

    @POST("booking/confirm")
    suspend fun confirmBooking(@Body request: ConfirmBookingRequest): Booking

    @GET("booking/history")
    suspend fun getBookingHistory(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): PaginatedResponse<Booking>

    @GET("booking")
    suspend fun getBookings(): List<Booking>

    @GET("booking/{id}")
    suspend fun getBookingById(@Path("id") id: String): Booking

    @POST("booking")
    suspend fun createBooking(@Body request: CreateBookingRequest): Booking

    @PUT("booking/{id}")
    suspend fun updateBooking(@Path("id") id: String, @Body request: UpdateBookingRequest): Booking

    @DELETE("booking/{id}")
    suspend fun cancelBooking(@Path("id") id: String): Booking

    // --- PAYMENT --- //
    @GET("payment/history")
    suspend fun getPaymentHistory(): List<Payment>

    @POST("payment/record")
    suspend fun recordPayment(@Body request: Map<String, Any>): Payment

    // --- PAYPAL PAYMENT --- //
    @POST("payment/paypal/create")
    suspend fun createPaypalOrder(@Body request: PaypalOrderRequest): PaypalOrderResponse

    @POST("payment/paypal/capture/{orderId}")
    suspend fun capturePaypalOrder(@Path("orderId") orderId: String): PaypalCaptureResponse

    @GET("payment/paypal/status/{orderId}")
    suspend fun getPaypalOrder(@Path("orderId") orderId: String): PaypalOrderStatusResponse

    // --- ONBOARDING --- //
    @POST("onboarding/start")
    suspend fun startOnboarding(): OnboardingResponse

    @POST("onboarding/answer")
    suspend fun submitAnswer(@Body answer: AnswerRequest): OnboardingResponse

    @GET("onboarding/status")
    suspend fun getOnboardingStatus(): OnboardingStatus

    @POST("onboarding/resume")
    suspend fun resumeOnboarding(@Body request: ResumeRequest): OnboardingResponse

    @POST("onboarding/skip")
    suspend fun skipOnboarding(): OnboardingResponse

    @POST("onboarding/reset")
    suspend fun resetOnboarding(): OnboardingResponse

    // --- RECOMMENDATIONS --- //
    @GET("recommendations/personalized")
    suspend fun getPersonalizedRecommendations(
        @Query("type") type: String = "all",
        @Query("limit") limit: Int = 10
    ): PersonalizedRecommendations

    @GET("recommendations/regenerate")
    suspend fun regenerateRecommendations(): PersonalizedRecommendations

    // --- CATALOG --- //
    @GET("catalog/recommended")
    suspend fun getRecommendedFlights(
        @Query("originLocationCode") originLocationCode: String? = null,
        @Query("destinationLocationCode") destinationLocationCode: String? = null,
        @Query("departureDate") departureDate: String? = null,
        @Query("returnDate") returnDate: String? = null,
        @Query("adults") adults: Int? = null,
        @Query("travelClass") travelClass: String? = null,
        @Query("currencyCode") currencyCode: String? = null,
        @Query("maxResults") maxResults: Int? = null,
        @Query("maxPrice") maxPrice: Double? = null
    ): RecommendedFlightsResponse

    @GET("catalog/explore")
    suspend fun getExploreOffers(
        @Query("origin") origin: String? = null,
        @Query("destination") destination: String? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("budget") budget: Int? = null,
        @Query("limit") limit: Int? = null
    ): ExploreOffersResponse

    @GET("catalog/activities")
    suspend fun getActivities(
        @Query("city") city: String? = null,
        @Query("themes") themes: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("radiusMeters") radiusMeters: Int? = null
    ): ActivityFeedResponse

    // --- HOTELS --- //
    @GET("catalog/hotels")
    suspend fun searchHotels(
        @Query("cityCode") cityCode: String,
        @Query("checkInDate") checkInDate: String,
        @Query("checkOutDate") checkOutDate: String,
        @Query("adults") adults: Int? = null,
        @Query("tripType") tripType: String? = null,
        @Query("ratings") ratings: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("currency") currency: String? = null
    ): HotelSearchResponse

    @GET("catalog/hotels/offers")
    suspend fun getHotelOffers(
        @Query("hotelIds") hotelIds: String,
        @Query("checkInDate") checkInDate: String,
        @Query("checkOutDate") checkOutDate: String,
        @Query("adults") adults: Int? = null,
        @Query("currency") currency: String? = null
    ): HotelOffersResponse

    @GET("catalog/hotels/{hotelId}")
    suspend fun getHotelById(@Path("hotelId") hotelId: String): HotelDetailResponse

    @GET("catalog/hotels/{hotelId}/reviews")
    suspend fun getHotelReviews(
        @Path("hotelId") hotelId: String,
        @Query("placeId") placeId: String?
    ): HotelReviewsResponse

    // --- DISCUSSION --- //
    @GET("discussion/posts")
    suspend fun getPosts(
        @Query("limit") limit: Int? = null,
        @Query("skip") skip: Int? = null,
        @Query("destination") destination: String? = null
    ): PostsResponse

    @GET("discussion/posts/{id}")
    suspend fun getPost(@Path("id") id: String): DiscussionPost

    @POST("discussion/posts")
    suspend fun createPost(@Body request: CreatePostRequest): DiscussionPost

    @PUT("discussion/posts/{id}")
    suspend fun updatePost(@Path("id") id: String, @Body request: CreatePostRequest): DiscussionPost

    @DELETE("discussion/posts/{id}")
    suspend fun deletePost(@Path("id") id: String): Map<String, String>

    @POST("discussion/posts/{id}/like")
    suspend fun likePost(@Path("id") id: String): DiscussionPost

    @GET("discussion/posts/{id}/comments")
    suspend fun getComments(
        @Path("id") postId: String,
        @Query("limit") limit: Int? = null,
        @Query("skip") skip: Int? = null
    ): CommentsResponse

    @POST("discussion/posts/{id}/comments")
    suspend fun createComment(@Path("id") postId: String, @Body request: CreateCommentRequest): DiscussionComment

    @POST("discussion/comments/{id}/like")
    suspend fun likeComment(@Path("id") commentId: String): DiscussionComment

    @DELETE("discussion/comments/{id}")
    suspend fun deleteComment(@Path("id") commentId: String): Map<String, String>

    // --- FAVORITES --- //
    @GET("favorites")
    suspend fun getFavorites(@Query("type") type: String? = null): List<Favorite>

    @GET("favorites/count")
    suspend fun getFavoriteCount(@Query("type") type: String? = null): FavoriteCountResponse

    @GET("favorites/check/{type}/{id}")
    suspend fun checkFavorite(@Path("type") type: String, @Path("id") id: String): FavoriteCheckResponse

    @POST("favorites")
    suspend fun addFavorite(@Body request: CreateFavoriteRequest): Favorite

    @DELETE("favorites/{type}/{id}")
    suspend fun removeFavorite(@Path("type") type: String, @Path("id") id: String): Map<String, String>

    // --- REVIEWS --- //
    @GET("reviews/{itemType}/{itemId}")
    suspend fun getReviews(@Path("itemType") itemType: String, @Path("itemId") itemId: String): List<Review>

    @GET("reviews/{itemType}/{itemId}/stats")
    suspend fun getReviewStats(@Path("itemType") itemType: String, @Path("itemId") itemId: String): ReviewStatsResponse

    @GET("reviews/check/{itemType}/{itemId}")
    suspend fun checkUserReview(@Path("itemType") itemType: String, @Path("itemId") itemId: String): Review?

    @GET("reviews/user/my-reviews")
    suspend fun getUserReviews(@Query("type") itemType: String? = null): List<Review>

    @POST("reviews")
    suspend fun createReview(@Body request: CreateReviewRequest): Review

    @PUT("reviews/{id}")
    suspend fun updateReview(@Path("id") reviewId: String, @Body request: UpdateReviewRequest): Review

    @DELETE("reviews/{id}")
    suspend fun deleteReview(@Path("id") reviewId: String): Map<String, String>

    // --- ITINERARY --- //
    @GET("itinerary")
    suspend fun getItineraries(@Query("includePublic") includePublic: Boolean = false): List<Itinerary>

    @GET("itinerary/{id}")
    suspend fun getItinerary(@Path("id") id: String): Itinerary

    @POST("itinerary")
    suspend fun createItinerary(@Body request: CreateItineraryRequest): Itinerary

    @PUT("itinerary/{id}")
    suspend fun updateItinerary(@Path("id") id: String, @Body request: UpdateItineraryRequest): Itinerary

    @DELETE("itinerary/{id}")
    suspend fun deleteItinerary(@Path("id") id: String): Map<String, String>

    @POST("itinerary/{id}/days/{dayDate}/activities")
    suspend fun addActivity(
        @Path("id") id: String,
        @Path("dayDate") dayDate: String,
        @Body activity: AddActivityRequest
    ): Itinerary

    @DELETE("itinerary/{id}/days/{dayDate}/activities/{activityIndex}")
    suspend fun removeActivity(
        @Path("id") id: String,
        @Path("dayDate") dayDate: String,
        @Path("activityIndex") activityIndex: Int
    ): Itinerary

    // --- NOTIFICATIONS --- //
    @GET("notifications")
    suspend fun getNotifications(@Query("unreadOnly") unreadOnly: Boolean = false): List<Notification>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): UnreadCountResponse

    @POST("notifications")
    suspend fun createNotification(@Body request: CreateNotificationRequest): Notification

    @PUT("notifications/{id}/read")
    suspend fun markAsRead(@Path("id") id: String): Notification

    @PUT("notifications/read-all")
    suspend fun markAllAsRead(): Map<String, String>

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: String): Map<String, String>

    @DELETE("notifications")
    suspend fun deleteAllNotifications(): Map<String, String>

    // --- SOCIAL --- //
    @POST("social/follow")
    suspend fun followUser(@Body request: FollowUserRequest): FollowResponse

    @POST("social/unfollow")
    suspend fun unfollowUser(@Body request: FollowUserRequest): FollowResponse

    @GET("social/follow-status/{userId}")
    suspend fun checkFollowStatus(@Path("userId") userId: String): FollowStatusResponse

    @GET("social/followers")
    suspend fun getFollowers(@Query("limit") limit: Int = 50, @Query("skip") skip: Int = 0): List<UserPreview>

    @GET("social/following")
    suspend fun getFollowing(@Query("limit") limit: Int = 50, @Query("skip") skip: Int = 0): List<UserPreview>

    @GET("social/follow-counts")
    suspend fun getFollowCounts(): FollowCountsResponse

    @GET("social/follow-counts/{userId}")
    suspend fun getFollowCountsByUserId(@Path("userId") userId: String): FollowCountsResponse

    @POST("social/share-trip")
    suspend fun shareTrip(@Body request: ShareTripRequest): SharedTrip

    @PUT("social/share-trip/{id}")
    suspend fun updateSharedTrip(@Path("id") id: String, @Body request: UpdateSharedTripRequest): SharedTrip

    @DELETE("social/share-trip/{id}")
    suspend fun deleteSharedTrip(@Path("id") id: String): Map<String, String>

    @GET("social/share-trip/{id}")
    suspend fun getSharedTrip(@Path("id") id: String): SharedTrip

    @GET("social/user/{userId}/shared-trips")
    suspend fun getUserSharedTrips(@Path("userId") userId: String, @Query("limit") limit: Int = 20, @Query("skip") skip: Int = 0): List<SharedTrip>

    @GET("social/feed")
    suspend fun getSocialFeed(@Query("limit") limit: Int = 20, @Query("skip") skip: Int = 0): List<SharedTrip>

    @POST("social/share-trip/{id}/like")
    suspend fun likeSharedTrip(@Path("id") id: String): LikeResponse

    @GET("social/map-memories")
    suspend fun getMapMemories(): MapMemoriesResponse

    // --- CONFIG --- //
    @GET("config/google-maps-api-key")
    suspend fun getGoogleMapsApiKey(): GoogleMapsApiKeyResponse

    // --- SEARCH HISTORY --- //
    @POST("search-history")
    suspend fun recordSearch(@Body request: CreateSearchHistoryRequest): SearchHistory

    @GET("search-history/recent")
    suspend fun getRecentSearches(@Query("type") searchType: String? = null, @Query("limit") limit: Int = 20, @Query("skip") skip: Int = 0): List<SearchHistory>

    @GET("search-history/saved")
    suspend fun getSavedSearches(@Query("type") searchType: String? = null, @Query("limit") limit: Int = 50, @Query("skip") skip: Int = 0): List<SearchHistory>

    @POST("search-history/{id}/save")
    suspend fun saveSearch(@Path("id") id: String, @Body request: SaveSearchRequest): SearchHistory

    @POST("search-history/{id}/unsave")
    suspend fun unsaveSearch(@Path("id") id: String): SearchHistory

    @DELETE("search-history/{id}")
    suspend fun deleteSearchHistory(@Path("id") id: String): Map<String, String>

    @DELETE("search-history/recent/clear")
    suspend fun clearRecentSearches(@Query("type") searchType: String? = null): Map<String, String>

    @GET("search-history/stats")
    suspend fun getSearchStats(): SearchStatsResponse

    // --- PRICE ALERTS --- //
    @POST("price-alerts")
    suspend fun createPriceAlert(@Body request: CreatePriceAlertRequest): PriceAlert

    @GET("price-alerts")
    suspend fun getPriceAlerts(@Query("activeOnly") activeOnly: Boolean = false): List<PriceAlert>

    @GET("price-alerts/{id}")
    suspend fun getPriceAlert(@Path("id") id: String): PriceAlert

    @PUT("price-alerts/{id}")
    suspend fun updatePriceAlert(@Path("id") id: String, @Body request: UpdatePriceAlertRequest): PriceAlert

    @DELETE("price-alerts/{id}")
    suspend fun deletePriceAlert(@Path("id") id: String): Map<String, String>

    @POST("price-alerts/{id}/deactivate")
    suspend fun deactivatePriceAlert(@Path("id") id: String): PriceAlert

    // --- TRAVEL TIPS --- //
    @GET("travel-tips")
    suspend fun getTravelTips(
        @Query("destinationId") destinationId: String,
        @Query("category") category: String? = null,
        @Query("limit") limit: Int = 10
    ): List<TravelTip>

    @GET("travel-tips/generate/{destinationId}")
    suspend fun generateTravelTips(
        @Path("destinationId") destinationId: String,
        @Query("destinationName") destinationName: String,
        @Query("city") city: String? = null,
        @Query("country") country: String? = null
    ): List<TravelTip>

    @POST("travel-tips")
    suspend fun createTravelTip(@Body request: CreateTravelTipRequest): TravelTip

    @POST("travel-tips/{tipId}/helpful")
    suspend fun markTipHelpful(@Path("tipId") tipId: String): TravelTip

    @GET("travel-tips/{tipId}")
    suspend fun getTravelTipById(@Path("tipId") tipId: String): TravelTip

    // --- JOURNEY --- //
    @Multipart
    @POST("journey")
    suspend fun createJourney(
        @Part images: List<MultipartBody.Part>,
        @Part("booking_id") bookingId: okhttp3.RequestBody? = null,
        @Part("destination") destination: okhttp3.RequestBody? = null,
        @Part("description") description: okhttp3.RequestBody? = null,
        @Part("tags") tags: okhttp3.RequestBody? = null,
        @Part("is_public") isPublic: okhttp3.RequestBody? = null
    ): Journey

    @GET("journey")
    suspend fun getJourneys(
        @Query("limit") limit: Int? = null,
        @Query("skip") skip: Int? = null
    ): List<Journey>

    @GET("journey/my-journeys")
    suspend fun getMyJourneys(
        @Query("limit") limit: Int? = null,
        @Query("skip") skip: Int? = null
    ): List<Journey>

    @GET("journey/can-share")
    suspend fun canShareJourney(): CanShareJourneyResponse

    @GET("journey/{id}")
    suspend fun getJourneyById(@Path("id") id: String): Journey

    @PUT("journey/{id}")
    suspend fun updateJourney(@Path("id") id: String, @Body request: UpdateJourneyRequest): Journey

    @DELETE("journey/{id}")
    suspend fun deleteJourney(@Path("id") id: String): Map<String, String>

    @POST("journey/{id}/like")
    suspend fun likeJourney(@Path("id") id: String): JourneyLikeResponse

    @POST("journey/{id}/comments")
    suspend fun addJourneyComment(@Path("id") id: String, @Body request: CreateJourneyCommentRequest): JourneyComment

    // --- GROUP FLIGHTS --- //
    @POST("group-flight")
    suspend fun createGroupFlight(@Body request: CreateGroupFlightRequest): GroupFlight
    
    @GET("group-flight")
    suspend fun getGroupFlights(): List<GroupFlight>
    
    @GET("group-flight/my-groups")
    suspend fun getMyGroupFlights(): List<GroupFlight>
    
    @GET("group-flight/{id}")
    suspend fun getGroupFlightById(@Path("id") id: String): GroupFlight
    
    @POST("group-flight/{id}/join")
    suspend fun joinGroupFlight(@Path("id") id: String, @Body request: JoinGroupFlightRequest): GroupFlight
    
    @POST("group-flight/{id}/invite")
    suspend fun inviteToGroupFlight(@Path("id") id: String, @Body request: Map<String, List<String>>): GroupFlight
    
    @DELETE("group-flight/{id}")
    suspend fun cancelGroupFlight(@Path("id") id: String): Map<String, String>
    
    @GET("group-flight/invitations")
    suspend fun getGroupFlightInvitations(): List<GroupFlightInvitation>
    
    @POST("group-flight/invitation/{id}/accept")
    suspend fun acceptGroupFlightInvitation(@Path("id") id: String): GroupFlight
    
    @POST("group-flight/invitation/{id}/decline")
    suspend fun declineGroupFlightInvitation(@Path("id") id: String): Map<String, String>
    
    @GET("group-flight/{id}/cost-breakdown")
    suspend fun getGroupFlightCostBreakdown(@Path("id") id: String): GroupFlightCostBreakdown
    
    @GET("journey/{id}/comments")
    suspend fun getJourneyComments(
        @Path("id") id: String,
        @Query("limit") limit: Int? = null,
        @Query("skip") skip: Int? = null
    ): List<JourneyComment>

    @DELETE("journey/comments/{commentId}")
    suspend fun deleteJourneyComment(@Path("commentId") commentId: String): Map<String, String>

    @POST("journey/{id}/regenerate-video")
    suspend fun regenerateJourneyVideo(@Path("id") id: String): Map<String, String>

    // --- DESTINATION VIDEO --- //
    @POST("users/{userId}/destinations/{destination}/generate-video")
    suspend fun generateDestinationVideo(
        @Path("userId") userId: String,
        @Path("destination") destination: String
    ): GenerateVideoResponse

    @GET("users/{userId}/destinations/{destination}/video-status")
    suspend fun getDestinationVideoStatus(
        @Path("userId") userId: String,
        @Path("destination") destination: String
    ): DestinationVideoStatus

    @GET("users/{userId}/destinations")
    suspend fun getUserDestinations(@Path("userId") userId: String): UserDestinationsResponse

    // --- AI TRAVEL VIDEO --- //
    @GET("ai-video/status")
    suspend fun getAiVideoStatus(): AiVideoStatusResponse

    @GET("ai-video/suggestions")
    suspend fun getAiVideoSuggestions(): AiVideoSuggestionsResponse

    @POST("ai-video/generate")
    suspend fun generateAiTravelVideo(@Body request: AiVideoGenerateRequest): AiVideoGenerateResponse

    @GET("ai-video/status/{predictionId}")
    suspend fun checkAiVideoStatus(@Path("predictionId") predictionId: String): AiVideoCheckStatusResponse

    @POST("ai-video/cancel/{predictionId}")
    suspend fun cancelAiVideo(@Path("predictionId") predictionId: String): GenericResponse

    @GET("ai-video/music-tracks")
    suspend fun getMusicTracks(): MusicTracksResponse

    @GET("ai-video/travel-plans")
    suspend fun getTravelPlans(): TravelPlansResponse

    @POST("ai-video/generate-with-media")
    suspend fun generateAiTravelVideoWithMedia(@Body request: AiVideoGenerateWithMediaRequest): AiVideoGenerateWithMediaResponse

    // --- CHAT --- //
    @POST("chat/message")
    suspend fun sendChatMessage(@Body request: ChatMessageRequest): ChatMessageResponse

    @POST("chat/switch-model")
    suspend fun switchChatModel(@Body request: SwitchModelRequest): SwitchModelResponse

    @GET("chat/history")
    suspend fun getChatHistory(@Query("limit") limit: Int = 50): List<ChatHistoryItem>

    @DELETE("chat/history")
    suspend fun clearChatHistory(): ClearHistoryResponse

    @GET("chat/models")
    suspend fun getAvailableModels(): AvailableModelsResponse

    // --- OUTFIT WEATHER --- //
    @Multipart
    @POST("outfit-weather/upload")
    suspend fun uploadOutfitImage(
        @Part image: MultipartBody.Part,
        @Part("booking_id") bookingId: okhttp3.RequestBody
    ): UploadOutfitResponse

    @POST("outfit-weather/analyze")
    suspend fun analyzeOutfit(@Body request: AnalyzeOutfitRequest): Outfit

    @GET("outfit-weather/booking/{bookingId}")
    suspend fun getOutfitsForBooking(@Path("bookingId") bookingId: String): List<Outfit>

    @GET("outfit-weather/{outfitId}")
    suspend fun getOutfit(@Path("outfitId") outfitId: String): Outfit

    @POST("outfit-weather/{outfitId}/approve")
    suspend fun approveOutfit(@Path("outfitId") outfitId: String): Outfit

    @DELETE("outfit-weather/{outfitId}")
    suspend fun deleteOutfit(@Path("outfitId") outfitId: String): Map<String, String>

    // --- REWARDS / POINTS --- //
    @GET("rewards/points")
    suspend fun getUserPoints(): UserPointsResponse

    // --- UPSELLS --- //
    @GET("upsells/products")
    suspend fun getUpsellProducts(
        @Query("destinationId") destinationId: String? = null,
        @Query("dates") dates: String? = null
    ): UpsellProductsResponse
}
