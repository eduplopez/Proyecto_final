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

    class Meta:
        unique_together = ('user', 'league')
