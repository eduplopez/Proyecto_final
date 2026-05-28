import json
import uuid
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.contrib.auth import authenticate
from .models import User, League, LeagueParticipant, Event

def token_required(view_func):
    def wrapper(request, *args, **kwargs):
        auth_header = request.headers.get('Authorization')
        if not auth_header or not auth_header.startswith('Bearer '):
            return JsonResponse({'error': 'No auth token provided'}, status=401)
        token = auth_header.split('Bearer ')[1]
        try:
            user = User.objects.get(token=token)
            request.user = user
        except User.DoesNotExist:
            return JsonResponse({'error': 'Invalid token'}, status=401)
        return view_func(request, *args, **kwargs)
    return wrapper

@csrf_exempt
def auth_register(request):
    if request.method == 'POST':
        try:
            data = json.loads(request.body)
            username = data.get('username')
            password = data.get('password')
            first_name = data.get('first_name', '')
            profile_photo = data.get('profile_photo', '')
            
            if User.objects.filter(username=username).exists():
                return JsonResponse({'error': 'Username already exists'}, status=400)
            
            token = str(uuid.uuid4())
            user = User.objects.create_user(
                username=username,
                password=password,
                first_name=first_name,
                profile_photo=profile_photo,
                token=token
            )
            return JsonResponse({
                'message': 'User registered successfully',
                'token': token,
                'user': {'id': user.id, 'username': user.username, 'first_name': user.first_name, 'profile_photo': user.profile_photo}
            }, status=201)
        except Exception as e:
            return JsonResponse({'error': str(e)}, status=400)
    return JsonResponse({'error': 'Method not allowed'}, status=405)

@csrf_exempt
def auth_login(request):
    if request.method == 'POST':
        try:
            data = json.loads(request.body)
            username = data.get('username')
            password = data.get('password')
            user = authenticate(username=username, password=password)
            if user is not None:
                if not user.token:
                    user.token = str(uuid.uuid4())
                    user.save()
                return JsonResponse({
                    'message': 'Login successful',
                    'token': user.token,
                    'user': {'id': user.id, 'username': user.username, 'first_name': user.first_name, 'profile_photo': user.profile_photo}
                })
            return JsonResponse({'error': 'Invalid credentials'}, status=401)
        except Exception as e:
            return JsonResponse({'error': str(e)}, status=400)
    return JsonResponse({'error': 'Method not allowed'}, status=405)

@csrf_exempt
@token_required
def leagues_list_create(request):
    if request.method == 'GET':
        user_id_param = request.GET.get('user_id', request.user.id)
        participations = LeagueParticipant.objects.filter(user_id=user_id_param, is_accepted=True).select_related('league')
        leagues = [p.league for p in participations]
            
        data = []
        for l in leagues:
            data.append({
                'id': l.id,
                'name': l.name,
                'description': l.description,
                'end_date': l.end_date,
                'starting_points': l.starting_points,
                'creator_id': l.creator_id,
                'participant_count': l.participants.filter(is_accepted=True).count(),
                'user_points': l.participants.filter(user=request.user, is_accepted=True).first().current_points if l.participants.filter(user=request.user, is_accepted=True).exists() else 0
            })
        return JsonResponse({'leagues': data})
        
    elif request.method == 'POST':
        try:
            data = json.loads(request.body)
            league = League.objects.create(
                name=data.get('name'),
                description=data.get('description', ''),
                end_date=data.get('end_date'),
                starting_points=data.get('starting_points', 0),
                creator=request.user
            )
            LeagueParticipant.objects.create(
                user=request.user,
                league=league,
                current_points=league.starting_points,
                is_accepted=True
            )
            return JsonResponse({'message': 'League created', 'league_id': league.id}, status=201)
        except Exception as e:
            return JsonResponse({'error': str(e)}, status=400)

@csrf_exempt
@token_required
def league_detail(request, league_id):
    try:
        league = League.objects.get(id=league_id)
    except League.DoesNotExist:
        return JsonResponse({'error': 'League not found'}, status=404)
        
    if request.method == 'GET':
        participants = league.participants.filter(is_accepted=True).select_related('user')
        current_user_p = participants.filter(user=request.user).first()
        is_current_user_admin = current_user_p.is_admin if current_user_p else False
        
        parts_data = []
        for p in participants:
            parts_data.append({
                'user_id': p.user.id,
                'username': p.user.username,
                'first_name': p.user.first_name,
                'points': p.current_points,
                'is_admin': p.is_admin,
                'is_creator': p.user.id == league.creator_id
            })
        return JsonResponse({
            'league': {
                'id': league.id,
                'name': league.name,
                'description': league.description,
                'end_date': league.end_date,
                'starting_points': league.starting_points,
                'creator_id': league.creator_id,
                'is_creator': league.creator_id == request.user.id,
                'is_admin': is_current_user_admin
            },
            'participants': parts_data
        })
        
    elif request.method == 'PUT':
        if league.creator != request.user:
            return JsonResponse({'error': 'Only creator can edit'}, status=403)
        try:
            data = json.loads(request.body)
            league.name = data.get('name', league.name)
            league.description = data.get('description', league.description)
            league.end_date = data.get('end_date', league.end_date)
            league.save()
            return JsonResponse({'message': 'League updated'})
        except Exception as e:
            return JsonResponse({'error': str(e)}, status=400)
            
    elif request.method == 'DELETE':
        if league.creator != request.user:
            return JsonResponse({'error': 'Only creator can delete'}, status=403)
        league.delete()
        return JsonResponse({'message': 'League deleted'})

@csrf_exempt
@token_required
def league_invite(request, league_id):
    if request.method == 'POST':
        try:
            league = League.objects.get(id=league_id)
            if league.creator != request.user:
                return JsonResponse({'error': 'Only creator can invite'}, status=403)
                
            data = json.loads(request.body)
            invited_user_id = data.get('user_id')
            user_to_invite = User.objects.get(id=invited_user_id)
            
            p, created = LeagueParticipant.objects.get_or_create(
                user=user_to_invite,
                league=league,
                defaults={'current_points': league.starting_points}
            )
            if not created:
                return JsonResponse({'error': 'User already in league'}, status=400)
                
            return JsonResponse({'message': 'User invited successfully'}, status=201)
        except Exception as e:
            return JsonResponse({'error': str(e)}, status=400)
    return JsonResponse({'error': 'Method not allowed'}, status=405)

@csrf_exempt
@token_required
def leave_league(request, league_id):
    if request.method == 'POST':
        try:
            p = LeagueParticipant.objects.get(league_id=league_id, user=request.user)
            p.delete()
            return JsonResponse({'message': 'Leaved successfully'})
        except LeagueParticipant.DoesNotExist:
            return JsonResponse({'error': 'Not part of this league'}, status=400)
    return JsonResponse({'error': 'Method not allowed'}, status=405)

@csrf_exempt
@token_required
def user_invitations(request):
    if request.method == 'GET':
        invites = LeagueParticipant.objects.filter(user=request.user, is_accepted=False).select_related('league', 'league__creator')
        data = []
        for i in invites:
            data.append({
                'league_id': i.league.id,
                'league_name': i.league.name,
                'creator_name': i.league.creator.username
            })
        return JsonResponse({'invitations': data})
    return JsonResponse({'error': 'Method not allowed'}, status=405)

@csrf_exempt
@token_required
def respond_invitation(request, league_id):
    if request.method == 'POST':
        try:
            data = json.loads(request.body)
            action = data.get('action')
            p = LeagueParticipant.objects.get(user=request.user, league_id=league_id, is_accepted=False)
            if action == 'accept':
                p.is_accepted = True
                p.save()
                return JsonResponse({'message': 'Invitation accepted'})
            elif action == 'reject':
                p.delete()
                return JsonResponse({'message': 'Invitation rejected'})
            else:
                return JsonResponse({'error': 'Invalid action'}, status=400)
        except LeagueParticipant.DoesNotExist:
            return JsonResponse({'error': 'Invitation not found'}, status=404)
        except Exception as e:
            return JsonResponse({'error': str(e)}, status=400)
    return JsonResponse({'error': 'Method not allowed'}, status=405)

@csrf_exempt
@token_required
def users_list(request):
    if request.method == 'GET':
        search_query = request.GET.get('search', '')
        if search_query:
            users = User.objects.filter(username__icontains=search_query) | User.objects.filter(first_name__icontains=search_query)
        else:
            users = User.objects.all()
            
        users = users.exclude(id=request.user.id)
            
        data = [{'id': u.id, 'username': u.username, 'first_name': u.first_name, 'profile_photo': u.profile_photo} for u in users]
        return JsonResponse({'users': data})
    return JsonResponse({'error': 'Method not allowed'}, status=405)

@csrf_exempt
@token_required
def user_profile(request):
    if request.method == 'GET':
        league_count = LeagueParticipant.objects.filter(user=request.user, is_accepted=True).count()
        return JsonResponse({
            'user': {
                'id': request.user.id,
                'username': request.user.username,
                'first_name': request.user.first_name,
                'profile_photo': request.user.profile_photo
            },
            'league_count': league_count
        })
    elif request.method == 'PUT':
        try:
            data = json.loads(request.body)
            request.user.first_name = data.get('first_name', request.user.first_name)
            request.user.profile_photo = data.get('profile_photo', request.user.profile_photo)
            request.user.save()
            return JsonResponse({'message': 'Profile updated successfully'})
        except Exception as e:
            return JsonResponse({'error': str(e)}, status=400)
    return JsonResponse({'error': 'Method not allowed'}, status=405)

@csrf_exempt
@token_required
def make_admin(request, league_id, user_id):
    if request.method == 'PUT':
        try:
            league = League.objects.get(id=league_id)
            if league.creator != request.user:
                return JsonResponse({'error': 'Only creator can modify admins'}, status=403)
            
            p = LeagueParticipant.objects.get(league=league, user_id=user_id)
            
            data = json.loads(request.body)
            is_admin = data.get('is_admin', True)
            
            p.is_admin = is_admin
            p.save()
            return JsonResponse({'message': 'Admin status updated'})
        except (League.DoesNotExist, LeagueParticipant.DoesNotExist):
            return JsonResponse({'error': 'Not found'}, status=404)
        except Exception as e:
            return JsonResponse({'error': str(e)}, status=400)
    return JsonResponse({'error': 'Method not allowed'}, status=405)
@csrf_exempt
@token_required
def league_events(request, league_id):
    try:
        league = League.objects.get(id=league_id)
    except League.DoesNotExist:
        return JsonResponse({'error': 'League not found'}, status=404)
        
    if request.method == 'GET':
        events = Event.objects.filter(league=league)
        data = [{
            'id': e.id,
            'name': e.name,
            'description': e.description,
            'event_type': e.event_type,
            'reward_points': e.reward_points,
            'creator_id': e.creator.id,
            'status': e.status,
            'winner_id': e.winner.id if e.winner else None
        } for e in events]
        return JsonResponse({'events': data})
        
    elif request.method == 'POST':
        is_creator = league.creator == request.user
        is_admin = LeagueParticipant.objects.filter(league=league, user=request.user, is_admin=True).exists()
        if not (is_creator or is_admin):
            return JsonResponse({'error': 'Only admins can create events'}, status=403)
            
        data = json.loads(request.body)
        try:
            event = Event.objects.create(
                league=league,
                name=data.get('name'),
                description=data.get('description', ''),
                event_type=data.get('event_type'),
                reward_points=data.get('reward_points', 0),
                creator=request.user
            )
            return JsonResponse({'message': 'Event created', 'event_id': event.id}, status=201)
        except Exception as e:
            return JsonResponse({'error': str(e)}, status=400)

@csrf_exempt
@token_required
def update_event(request, league_id, event_id):
    if request.method == 'PUT':
        try:
            league = League.objects.get(id=league_id)
            is_creator = league.creator == request.user
            is_admin = LeagueParticipant.objects.filter(league=league, user=request.user, is_admin=True).exists()
            if not (is_creator or is_admin):
                return JsonResponse({'error': 'Only admins can update events'}, status=403)
                
            event = Event.objects.get(id=event_id, league=league)
            data = json.loads(request.body)
            
            winner_id = data.get('winner_id')
            if winner_id:
                winner = User.objects.get(id=winner_id)
                event.winner = winner
                event.status = 'FINISHED'
                event.save()
                
                p = LeagueParticipant.objects.get(league=league, user=winner)
                p.current_points += event.reward_points
                p.save()
            else:
                event.name = data.get('name', event.name)
                event.event_type = data.get('event_type', event.event_type)
                event.reward_points = data.get('reward_points', event.reward_points)
                event.save()
                
            return JsonResponse({'message': 'Event updated'})
        except Exception as e:
            return JsonResponse({'error': str(e)}, status=400)
    return JsonResponse({'error': 'Method not allowed'}, status=405)
