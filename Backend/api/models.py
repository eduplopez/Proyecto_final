from django.db import models
from django.contrib.auth.models import AbstractUser

class User(AbstractUser):
    profile_photo = models.CharField(max_length=50, default='avatar1', blank=True, null=True, help_text="Nombre del avatar predeterminado")
    token = models.CharField(max_length=100, blank=True, null=True, unique=True)

class League(models.Model):
    name = models.CharField(max_length=100)
    description = models.TextField(blank=True)
    end_date = models.DateField()
    starting_points = models.IntegerField(default=0)
    creator = models.ForeignKey(User, on_delete=models.CASCADE, related_name="created_leagues")

class LeagueParticipant(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name="participations")
    league = models.ForeignKey(League, on_delete=models.CASCADE, related_name="participants")
    current_points = models.IntegerField(default=0)
    is_accepted = models.BooleanField(default=False)
    is_admin = models.BooleanField(default=False)

    class Meta:
        unique_together = ('user', 'league')

class Event(models.Model):
    league = models.ForeignKey(League, on_delete=models.CASCADE, related_name="events")
    name = models.CharField(max_length=100)
    event_type = models.CharField(max_length=50) # Partido, Reto, Apuesta
    description = models.TextField(blank=True, null=True)
    reward_points = models.IntegerField(default=0)
    creator = models.ForeignKey(User, on_delete=models.CASCADE, related_name="created_events")
    status = models.CharField(max_length=50, default='PENDING') # PENDING, FINISHED
    winner = models.ForeignKey(User, on_delete=models.SET_NULL, null=True, blank=True, related_name="won_events")
