from django.urls import path
from . import views

urlpatterns = [
    path('auth/register/', views.auth_register),
    path('auth/login/', views.auth_login),
    path('leagues/', views.leagues_list_create),
    path('leagues/<int:league_id>/', views.league_detail),
    path('leagues/<int:league_id>/invite/', views.league_invite),
    path('users/', views.users_list),
    path('users/profile/', views.user_profile),
]
