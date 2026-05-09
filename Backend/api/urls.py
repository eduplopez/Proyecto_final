from django.urls import path
from . import views

urlpatterns = [
    path('auth/register/', views.auth_register),
    path('auth/login/', views.auth_login),
    path('leagues/', views.leagues_list_create),
    path('leagues/<int:league_id>/', views.league_detail),
    path('leagues/<int:league_id>/invite/', views.league_invite),
    path('leagues/<int:league_id>/leave/', views.leave_league),
    path('invitations/', views.user_invitations),
    path('leagues/<int:league_id>/respond/', views.respond_invitation),
    path('users/', views.users_list),
    path('users/profile/', views.user_profile),
]
