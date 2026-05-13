import pytest
from unittest.mock import MagicMock, patch
import jwt as pyjwt
from datetime import datetime, timedelta, timezone


def make_jwt(role: str, user_id: str = "550e8400-e29b-41d4-a716-446655440000") -> str:
    return pyjwt.encode(
        {"sub": user_id, "role": role, "exp": datetime.now(timezone.utc) + timedelta(hours=1)},
        "test-secret",
        algorithm="HS256",
    )


def test_decode_jwt_extracts_user_and_role():
    from session import decode_jwt
    token = make_jwt("CUSTOMER", "abc-123")
    with patch("session.settings") as mock_settings:
        mock_settings.jwt_secret = "test-secret"
        payload = decode_jwt(token)
    assert payload["sub"] == "abc-123"
    assert payload["role"] == "CUSTOMER"


def test_decode_jwt_raises_on_invalid():
    from session import decode_jwt
    with patch("session.settings") as mock_settings:
        mock_settings.jwt_secret = "test-secret"
        with pytest.raises(Exception):
            decode_jwt("not-a-valid-token")


def test_filter_tools_removes_admin_tools_for_customer():
    from session import filter_tools_for_role, ADMIN_ONLY_TOOLS

    def mock_tool(name):
        t = MagicMock()
        t.name = name
        return t

    all_tools = [mock_tool(n) for n in ["list_products", "place_order", "list_all_orders", "create_product"]]
    filtered = filter_tools_for_role(all_tools, "CUSTOMER")
    names = [t.name for t in filtered]
    assert "list_products" in names
    assert "place_order" in names
    for admin_tool in ADMIN_ONLY_TOOLS:
        assert admin_tool not in names


def test_filter_tools_allows_all_for_admin():
    from session import filter_tools_for_role

    def mock_tool(name):
        t = MagicMock()
        t.name = name
        return t

    all_tools = [mock_tool(n) for n in ["list_products", "list_all_orders", "create_product"]]
    filtered = filter_tools_for_role(all_tools, "ADMIN")
    assert len(filtered) == len(all_tools)
