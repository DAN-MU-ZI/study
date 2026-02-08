import zlib
import string
import random

def generate_short_url(long_url: str, length: int = 7) -> str:
    """
    URL 단축 해시 생성 (CRC32 + Hex)
    
    플로우:
    1. CRC32 해싱 (32bit 정수)
    2. Hex 문자열 변환 (최대 8자리)
    3. 7자리로 정규화 (앞 0 패딩)
    """
    # 1. CRC32 해싱 (32bit unsigned int)
    hash_int = zlib.crc32(long_url.encode()) & 0xffffffff
    
    # 2. Hex 변환 (0x 제거)
    encoded = hex(hash_int)[2:]
    
    # 3. 7자리로 정규화 (부족하면 0 채움)
    if len(encoded) < length:
        encoded = encoded.zfill(length)
    else:
        encoded = encoded[:length]
        
    return encoded

def generate_random_salt(length: int = 4) -> str:
    """충돌 해결을 위한 랜덤 솔트 생성 (Base62 Charset 사용)"""
    # 16진수와 섞여도 문제 없도록 영문 대소문자+숫자 사용
    chars = string.ascii_letters + string.digits
    return "".join(random.choices(chars, k=length))
