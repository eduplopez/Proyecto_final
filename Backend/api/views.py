import json
import uuid
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.contrib.auth import authenticate
from .models import User, League, LeagueParticipant

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
        user_id_param = request.GET.get('user_id')
        if user_id_param:
            participations = LeagueParticipant.objects.filter(user_id=user_id_param).select_related('league')
            leagues = [p.league for p in participations]
        else:
            leagues = League.objects.all()
            
        data = []
        for l in leagues:
            data.append({
                'id': l.id,
                'name': l.name,
                'description': l.description,
                'end_date': l.end_date,
                'starting_points': l.starting_points,
                'creator_id': l.creator_id
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
                current_points=league.starting_points
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
        participants = league.participants.all().select_related('user')
        parts_data = []
        for p in participants:
            parts_data.append({
                'user_id': p.user.id,
                'username': p.user.username,
                'first_name': p.user.first_name,
                'points': p.current_points
            })
        return JsonResponse({
            'league': {
                'id': league.id,
                'name': league.name,
                'description': league.description,
                'end_date': league.end_date,
                'starting_points': league.starting_points,
                'creator_id': league.creator_id
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
def users_list(request):
    if request.method == 'GET':
        search_query = request.GET.get('search', '')
        if search_query:
            users = User.objects.filter(username__icontains=search_query) | User.objects.filter(first_name__icontains=search_query)
        else:
            users = User.objects.all()
            
        data = [{'id': u.id, 'username': u.username, 'first_name': u.first_name, 'profile_photo': u.profile_photo} for u in users]
        return JsonResponse({'users': data})
    return JsonResponse({'error': 'Method not allowed'}, status=405)

@csrf_exempt
@token_required
def user_profile(request):
    if request.method == 'GET':
        league_count = LeagueParticipant.objects.filter(user=request.user).count()
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
